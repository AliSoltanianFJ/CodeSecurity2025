package main

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

var (
	imageDir = "images" // Define image directory as a global variable
)

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/img", imgHandler)

	server := &http.Server{
		Addr:         ":8080",
		Handler:      securityMiddleware(mux), // Apply security middleware
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
		MaxHeaderBytes: 8192,
	}

	fmt.Println("Server listening on :8080")
	err := server.ListenAndServe()
	if err != nil {
		fmt.Printf("Server failed to start: %v\n", err)
	}
}

// securityMiddleware applies security headers and other security measures
func securityMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Prevent MIME-sniffing
		w.Header().Set("X-Content-Type-Options", "nosniff")
		// Mitigate XSS attacks
		w.Header().Set("Content-Security-Policy", "default-src 'self'")
		// Prevent clickjacking
		w.Header().Set("X-Frame-Options", "DENY")
		// Prevent caching
		w.Header().Set("Cache-Control", "no-store")

		next.ServeHTTP(w, r)
	})
}

// imgHandler handles the image request
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

// isValidFilename validates the filename to prevent path traversal attacks
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