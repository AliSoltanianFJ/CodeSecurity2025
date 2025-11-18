package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

const maxFileSize = 10 * 1024 * 1024 // 10 MB

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Get the "file" parameter from the query string
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}

	// Sanitize the filename to prevent path traversal
	filename = filepath.Clean(filename)
	if strings.Contains(filename, "..") || strings.HasPrefix(filename, "/") || strings.HasPrefix(filename, "\\") {
		http.Error(w, "Invalid file name", http.StatusBadRequest)
		return
	}

	// Build the absolute path to the image directory
	imageDir := "images"
	absImageDir, err := filepath.Abs(imageDir)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Build the absolute path to the requested file
	absFilePath, err := filepath.Abs(filepath.Join(imageDir, filename))
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Ensure the requested file is within the images directory (prevents path traversal)
	if !strings.HasPrefix(absFilePath, absImageDir+string(os.PathSeparator)) && absFilePath != absImageDir {
		http.Error(w, "Forbidden", http.StatusForbidden)
		return
	}

	// Check if the file exists and is not a directory
	fileInfo, err := os.Stat(absFilePath)
	if err != nil || fileInfo.IsDir() {
		http.Error(w, "Not Found", http.StatusNotFound)
		return
	}

	// Set the appropriate Content-Type header based on the file extension
	contentType := mime.TypeByExtension(filepath.Ext(filename))
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	w.Header().Set("Content-Type", contentType)

	// Serve the file content with size limiting
	file, err := os.Open(absFilePath)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Limit the response body size to prevent DoS attacks
	limitedReader := io.LimitReader(file, maxFileSize)
	_, err = io.Copy(w, limitedReader)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
}
