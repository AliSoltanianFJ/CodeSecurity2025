package main

import (
	"context"
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
	mux := http.NewServeMux()
	mux.HandleFunc("/img", imgHandler)

	server := &http.Server{
		Addr:         ":8080",
		Handler:      securityHeaders(mux), // Apply security headers
		ReadTimeout:  5 * time.Second,     // Mitigate slowloris attacks
		WriteTimeout: 10 * time.Second,    // Mitigate slowloris attacks
		IdleTimeout:  120 * time.Second,   // Mitigate idle connections
	}

	log.Println("Server listening on :8080")
	log.Fatal(server.ListenAndServe())
}

func imgHandler(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	filename := r.URL.Query().Get("filename")
	if !isValidFilename(filename) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("images", filename)

	fileInfo, err := os.Stat(imagePath)
	if os.IsNotExist(err) || fileInfo.IsDir() {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	if err != nil {
		log.Println("File stat error:", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	file, err := os.Open(imagePath)
	if err != nil {
		log.Println("File open error:", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	contentType := detectContentType(file)
	w.Header().Set("Content-Type", contentType)

	// Use context-aware IO copy
	if _, err := io.Copy(w, file); err != nil {
		select {
		case <-ctx.Done():
			log.Println("Request timed out during file transfer")
			return // Client disconnected
		default:
			log.Println("File copy error:", err)
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}
	}

	fmt.Println("File served:", filename)
}

func isValidFilename(filename string) bool {
	if filename == "" || len(filename) > 255 {
		return false
	}
	cleanedFilename := filepath.Clean(filename)
	if strings.Contains(cleanedFilename, "..") || strings.Contains(cleanedFilename, "/") || strings.Contains(cleanedFilename, "\\") {
		return false
	}
	return true
}

func detectContentType(file *os.File) string {
	buffer := make([]byte, 512)
	_, err := file.Read(buffer)
	if err != nil && err != io.EOF {
		log.Println("Error reading file:", err)
		return "application/octet-stream"
	}
	_, err = file.Seek(0, io.SeekStart)
	if err != nil {
		log.Println("Error seeking file:", err)
		return "application/octet-stream"
	}
	return http.DetectContentType(buffer)
}

// securityHeaders middleware
func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("Content-Security-Policy", "default-src 'self'")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("X-XSS-Protection", "1; mode=block")
		next.ServeHTTP(w, r)
	})
}