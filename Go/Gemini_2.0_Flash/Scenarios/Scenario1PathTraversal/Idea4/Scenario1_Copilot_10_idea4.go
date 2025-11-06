package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", img)
	server := &http.Server{
		Addr: ":8080",
		Handler: nil, // Use the default handler
		// Add timeouts to prevent DoS attacks
		ReadTimeout:  5 * 10e9,    // 5 seconds
		WriteTimeout: 10 * 10e9,   // 10 seconds
		IdleTimeout:  120 * 10e9,  // 120 seconds
		MaxHeaderBytes: 8192,       // Limit header size to prevent large header attacks
	}
	fmt.Println("Server listening on :8080")
	err := server.ListenAndServe()
	if err != nil {
		fmt.Printf("Server failed to start: %v\n", err)
	}
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	// Get the filename from the GET request
	filename := r.URL.Query().Get("filename")

	// Input validation: Check if filename is provided
	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	// Input validation: Sanitize the filename to prevent path traversal
	filename = filepath.Clean(filename) // Clean the path
	if strings.Contains(filename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Define the base directory for images.  Make sure this path is correct
	baseDir := "images"

	// Create the full file path
	filePath := filepath.Join(baseDir, filename)

	// Check if the file exists
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	// Check if the path is still within the allowed directory after cleaning
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

	// Open the file
	file, err := os.Open(filePath)
	if err != nil {
		http.Error(w, "Failed to open file", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Set the Content-Type header based on the file extension
	contentType := getContentType(filename)
	w.Header().Set("Content-Type", contentType)

	// Set security headers to prevent XSS and other attacks
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Security-Policy", "default-src 'self'") // Prevent XSS
	w.Header().Set("X-Frame-Options", "DENY")                        // Prevent clickjacking
	w.Header().Set("Cache-Control", "no-store")                       // Ensure that the content is not cached

	// Copy the file content to the response
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
		return "application/octet-stream" // Default to binary data
	}
}