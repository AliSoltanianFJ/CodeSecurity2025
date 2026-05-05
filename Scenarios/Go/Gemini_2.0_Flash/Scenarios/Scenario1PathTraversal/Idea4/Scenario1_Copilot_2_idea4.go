package main

import (
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/img", img)

	// Configure TLS
	tlsConfig := &tls.Config{
		// Enforce TLS 1.3
		MinVersion: tls.VersionTLS13,
		// Only allow secure cipher suites
		CipherSuites: []uint16{
			tls.TLS_AES_128_GCM_SHA256,
			tls.TLS_AES_256_GCM_SHA384,
			tls.TLS_CHACHA20_POLY1305_SHA256,
		},
		PreferServerCipherSuites: true,
		// Ensure only authorized clients can connect (if applicable)
		ClientAuth: tls.NoClientCert, // or tls.RequireAndVerifyClientCert
	}

	server := &http.Server{
		Addr:         ":8080",
		Handler:      nil, // Use the default handler
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
		MaxHeaderBytes: 8192,
		TLSConfig:    tlsConfig,
		ErrorLog:     log.New(os.Stderr, "http: ", log.LstdFlags), // Log errors
	}

	fmt.Println("Server listening on :8080")

	// Listen and Serve TLS (HTTPS)
	err := server.ListenAndServeTLS("cert.pem", "key.pem")
	if err != nil {
		fmt.Printf("Server failed to start: %v\n", err)
	}
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	// Rate limiting to prevent abuse
	// (Example: limit to 10 requests per minute per IP)
	// Implement a proper rate limiting mechanism using a library or middleware

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

	// Validate file existence
	fileInfo, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	// Prevent directory listing
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

	// Implement logging of file access (optional)
	// log.Printf("File served: %s from IP: %s", filename, r.RemoteAddr)

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