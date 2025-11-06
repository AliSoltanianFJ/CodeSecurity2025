package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

// Rate limiter parameters
const (
	maxRequestsPerMinute = 20  // Adjust as needed
	cleanupInterval      = 10 * time.Minute // Interval to cleanup expired entries
)

// ipRateLimiter stores the number of requests per IP address
type ipRateLimiter struct {
	mu          sync.Mutex
	requestCounts map[string]int
	lastRequestTimes map[string]time.Time
}

// newIPRateLimiter creates a new rate limiter
func newIPRateLimiter() *ipRateLimiter {
	limiter := &ipRateLimiter{
		requestCounts:  make(map[string]int),
		lastRequestTimes: make(map[string]time.Time),
	}

	// Start a cleanup goroutine to remove expired entries
	go limiter.cleanupExpired()

	return limiter
}

// isAllowed checks if the IP address is allowed to make a request
func (l *ipRateLimiter) isAllowed(ip string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := time.Now()
	count := l.requestCounts[ip]

	// Check if the IP has exceeded the limit
	if count >= maxRequestsPerMinute {
		return false
	}

	// Update the request count and last request time
	l.requestCounts[ip]++
	l.lastRequestTimes[ip] = now

	return true
}

// cleanupExpired removes expired entries from the rate limiter
func (l *ipRateLimiter) cleanupExpired() {
	ticker := time.NewTicker(cleanupInterval)
	defer ticker.Stop()

	for range ticker.C {
		l.mu.Lock()
		for ip, lastRequestTime := range l.lastRequestTimes {
			// Remove entries that have not been updated in the last minute
			if time.Since(lastRequestTime) > time.Minute {
				delete(l.requestCounts, ip)
				delete(l.lastRequestTimes, ip)
			}
		}
		l.mu.Unlock()
	}
}

var limiter = newIPRateLimiter()

func main() {
	http.HandleFunc("/img", img)

	server := &http.Server{
		Addr:         ":8080",
		Handler:      http.HandlerFunc(rateLimitMiddleware), // Apply rate limiting middleware
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
		MaxHeaderBytes: 8192,
		ErrorLog:     log.New(os.Stderr, "http: ", log.LstdFlags),
	}

	fmt.Println("Server listening on :8080")
	err := server.ListenAndServe()
	if err != nil {
		fmt.Printf("Server failed to start: %v\n", err)
	}
}

// rateLimitMiddleware applies rate limiting to the handler
func rateLimitMiddleware(w http.ResponseWriter, r *http.Request) {
	ip := getIP(r)

	if !limiter.isAllowed(ip) {
		http.Error(w, "Too many requests", http.StatusTooManyRequests)
		return
	}

	img(w, r)
}

// getIP retrieves the client IP address from the request
func getIP(r *http.Request) string {
	// Check for the X-Forwarded-For header to get the real IP address if the
	// server is behind a proxy or load balancer
	forwardedFor := r.Header.Get("X-Forwarded-For")
	if forwardedFor != "" {
		ips := strings.Split(forwardedFor, ",")
		// Return the first IP address in the list (the client's original IP)
		return strings.TrimSpace(ips[0])
	}

	// If X-Forwarded-For is not present, use the RemoteAddr
	ip := strings.Split(r.RemoteAddr, ":")[0]
	return ip
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")

	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	filename = filepath.Clean(filename)
	if strings.Contains(filename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	baseDir := "images"
	filePath := filepath.Join(baseDir, filename)

	fileInfo, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	if fileInfo.IsDir() {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	absPath, err := filepath.Abs(filePath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	absBaseDir, err := filepath.Abs(baseDir)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	if !strings.HasPrefix(absPath, absBaseDir) {
		http.Error(w, "Invalid path", http.StatusBadRequest)
		return
	}

	file, err := os.Open(filePath)
	if err != nil {
		http.Error(w, "Failed to open file", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	contentType := getContentType(filename)
	w.Header().Set("Content-Type", contentType)
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Security-Policy", "default-src 'self'")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("Cache-Control", "no-store")

	_, err = io.Copy(w, file)
	if err != nil {
		http.Error(w, "Failed to send file", http.StatusInternalServerError)
		return
	}
}

func getContentType(filename string) string {
	ext := strings.ToLower(filepath.Ext(filename))
	switch ext {
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".gif":
		return "image/gif"
	case ".bmp":
		return "image/bmp"
	case ".webp":
		return "image/webp"
	default:
		return "application/octet-stream"
	}
}