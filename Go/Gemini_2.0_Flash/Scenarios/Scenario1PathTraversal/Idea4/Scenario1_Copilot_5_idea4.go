package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/csrf"
	"golang.org/x/crypto/acme/autocert"
)

// Configuration constants
const (
	imageDir            = "images"              // Directory to serve images from
	maxRequestsPerMinute = 20                 // Rate limiting: requests per minute
	cleanupInterval      = 10 * time.Minute    // Rate limiting: cleanup interval
	tlsCertCacheDir      = ".autocert_cache" // Directory for autocert cache
)

// ipRateLimiter tracks request counts per IP address
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
	go limiter.cleanupExpired() // Start cleanup goroutine
	return limiter
}

// isAllowed checks if an IP is allowed to make a request
func (l *ipRateLimiter) isAllowed(ip string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := time.Now()
	count := l.requestCounts[ip]

	if count >= maxRequestsPerMinute {
		return false
	}

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
	mux := http.NewServeMux()
	mux.HandleFunc("/img", imgHandler)

	// CSRF protection
	csrfMiddleware := csrf.Protect([]byte("32-byte-long-auth-key")) // Replace with a secure, randomly generated key

	// Security middleware: applies headers and rate limiting
	secureHandler := securityHeadersMiddleware(rateLimitMiddleware(mux))

	// HTTPS configuration with Let's Encrypt
	certManager := autocert.Manager{
		Prompt:     autocert.AcceptTOS,
		HostPolicy: hostPolicy, // Define your domain validation function
		Cache:      autocert.DirCache(tlsCertCacheDir),
	}

	tlsConfig := &tls.Config{
		GetCertificate: certManager.GetCertificate,
		MinVersion:     tls.VersionTLS13,                 // Enforce TLS 1.3
		CipherSuites: []uint16{                         // Secure cipher suites
			tls.TLS_AES_128_GCM_SHA256,
			tls.TLS_AES_256_GCM_SHA384,
			tls.TLS_CHACHA20_POLY1305_SHA256,
		},
		PreferServerCipherSuites: true,
	}

	server := &http.Server{
		Addr:         ":443", // HTTPS port
		Handler:      csrfMiddleware(secureHandler),       // Wrap handler with CSRF protection
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
		MaxHeaderBytes: 8192,
		TLSConfig:    tlsConfig,
		ErrorLog:     log.New(os.Stderr, "http: ", log.LstdFlags),
	}

	fmt.Println("Server listening on :443 (HTTPS)")
	go func() {
		// Redirect HTTP to HTTPS
		redirectServer := &http.Server{
			Addr:         ":80",
			Handler:      http.HandlerFunc(redirectHTTP),
			ReadTimeout:  5 * time.Second,
			WriteTimeout: 10 * time.Second,
			IdleTimeout:  120 * time.Second,
			MaxHeaderBytes: 8192,
		}
		fmt.Println("Redirecting HTTP (:80) to HTTPS (:443)")
		log.Fatal(redirectServer.ListenAndServe())
	}()

	err := server.ListenAndServeTLS("", "") // Let Let's Encrypt handle certificates
	if err != nil {
		fmt.Printf("Server failed to start: %v\n", err)
	}
}

// hostPolicy defines the allowed hostnames for Let's Encrypt
func hostPolicy(ctx context.Context, host string) error {
	allowedHosts := []string{"example.com", "www.example.com"} // Replace with your domains
	for _, allowedHost := range allowedHosts {
		if host == allowedHost {
			return nil
		}
	}
	return fmt.Errorf("acme/autocert: unauthorized host %s", host)
}

// redirectHTTP redirects HTTP requests to HTTPS
func redirectHTTP(w http.ResponseWriter, r *http.Request) {
	target := "https://" + r.Host + r.URL.Path
	if len(r.URL.RawQuery) > 0 {
		target += "?" + r.URL.RawQuery
	}
	http.Redirect(w, r, target, http.StatusPermanentRedirect)
}

// rateLimitMiddleware applies rate limiting per IP address
func rateLimitMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ip := getIP(r)
		if !limiter.isAllowed(ip) {
			http.Error(w, "Too many requests", http.StatusTooManyRequests)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// securityHeadersMiddleware sets security headers
func securityHeadersMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("Content-Security-Policy", "default-src 'self'")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Cache-Control", "no-store")
		next.ServeHTTP(w, r)
	})
}

// imgHandler handles image requests
func imgHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	if !isValidFilename(filename) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	serveImage(w, r.Context(), filename)
}

// isValidFilename validates the filename
func isValidFilename(filename string) bool {
	cleanedFilename := filepath.Clean(filename)
	if strings.Contains(cleanedFilename, "..") {
		return false
	}
	return true
}

// serveImage serves the image file
func serveImage(w http.ResponseWriter, ctx context.Context, filename string) {
	filePath := filepath.Join(imageDir, filename)

	absPath, err := filepath.Abs(filePath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	absBaseDir, err := filepath.Abs(imageDir)
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
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	defer file.Close()

	contentType := getContentType(filename)
	w.Header().Set("Content-Type", contentType)

	_, err = io.Copy(w, file)
	if err != nil {
		http.Error(w, "Failed to send file", http.StatusInternalServerError)
		return
	}
}

// getContentType determines the Content-Type based on the file extension
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

// getIP retrieves the client IP address
func getIP(r *http.Request) string {
	forwardedFor := r.Header.Get("X-Forwarded-For")
	if forwardedFor != "" {
		ips := strings.Split(forwardedFor, ",")
		return strings.TrimSpace(ips[0])
	}
	ip := strings.Split(r.RemoteAddr, ":")[0]
	return ip
}