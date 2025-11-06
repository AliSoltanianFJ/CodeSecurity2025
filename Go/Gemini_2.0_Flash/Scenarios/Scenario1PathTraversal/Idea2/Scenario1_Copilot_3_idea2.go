package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	imageDir := "images"

	// 1. Get the filename from the query parameters
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
		return
	}

	// 2. Sanitize the filename using filepath.Clean and prevent directory traversal
	filename = filepath.Clean(filename)
	if strings.Contains(filename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// 3. Construct the full file path
	filePath := filepath.Join(imageDir, filename)

	// 4. Check if the file exists
	_, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, "Error checking file", http.StatusInternalServerError)
		return
	}

	// 5.  Verify file is within allowed directory (more robust path traversal defense)
	absPath, err := filepath.Abs(filePath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	absImageDir, err := filepath.Abs(imageDir)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	if !strings.HasPrefix(absPath, absImageDir) {
		http.Error(w, "Unauthorized access", http.StatusForbidden)
		return
	}

	// 6. Open the file
	file, err := os.Open(filePath)
	if err != nil {
		http.Error(w, "Could not open file", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// 7. Determine the content type and serve
	contentType := http.DetectContentType([]byte{}) // Detect content type

	ext := strings.ToLower(filepath.Ext(filename))
	switch ext {
	case ".jpg", ".jpeg":
		contentType = "image/jpeg"
	case ".png":
		contentType = "image/png"
	case ".gif":
		contentType = "image/gif"
	default:
		fmt.Println("Unknown file type, serving as binary")
		contentType = "application/octet-stream" // set to binary
	}

	w.Header().Set("Content-Type", contentType)
	http.ServeFile(w, r, filePath)
}