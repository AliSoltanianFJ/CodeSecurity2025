package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", imgHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func imgHandler(w http.ResponseWriter, r *http.Request) {
	// 1. Input Validation: Check if the filename parameter exists.
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "filename parameter is missing", http.StatusBadRequest)
		return
	}

	// 2. Input Sanitization: Sanitize the filename to prevent path traversal.
	filename = filepath.Clean(filename) // Removes ../ sequences
	if strings.Contains(filename, "/") || strings.Contains(filename, "\\") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// 3. Construct the file path.
	imagePath := filepath.Join("images", filename) // Assuming "images" is the root folder.

	// 4. Check if the file exists.
	fileInfo, err := os.Stat(imagePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// 5. Prevent directory listing.
	if fileInfo.IsDir() {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	// 6. Open the file.
	file, err := os.Open(imagePath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// 7. Detect content type to prevent MIME sniffing.
	contentType := detectContentType(file)
	w.Header().Set("Content-Type", contentType)

	// 8. Set security headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Security-Policy", "default-src 'self'")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-XSS-Protection", "1; mode=block")

	// 9. Serve the file.
	_, err = io.Copy(w, file)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Println("File served.")
}

func detectContentType(file *os.File) string {
	buffer := make([]byte, 512)
	_, err := file.Read(buffer)
	if err != nil && err != io.EOF {
		log.Println("Error reading file:", err)
		return "application/octet-stream" // Default to binary data
	}

	// Reset file read pointer
	_, err = file.Seek(0, io.SeekStart)
	if err != nil {
		log.Println("Error seeking file:", err)
		return "application/octet-stream"
	}

	contentType := http.DetectContentType(buffer)
	return contentType
}