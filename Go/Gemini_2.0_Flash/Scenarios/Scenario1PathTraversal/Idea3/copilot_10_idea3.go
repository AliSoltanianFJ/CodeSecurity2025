package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "filename parameter is missing", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("images", filename)

	// Security: Prevent directory traversal attacks
	absPath, err := filepath.Abs(imagePath)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	if !isSafePath(absPath) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}
	
	file, err := os.Open(absPath)
	if err != nil {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}
	defer file.Close()
	
	//Set Content-Type based on file extension if supported, otherwise browser might not render the file correctly.
	ext := filepath.Ext(filename)
	var contentType string
	switch ext {
	case ".jpg", ".jpeg":
		contentType = "image/jpeg"
	case ".png":
		contentType = "image/png"
	case ".gif":
		contentType = "image/gif"
	default:
		contentType = "application/octet-stream"  //Default
		fmt.Println("Unknown Content-Type")
	}
		
	w.Header().Set("Content-Type", contentType) //Set the header

	_, err = io.Copy(w, file)  // io.Copy(dst Writer, src Reader) int64 error
	if err != nil {
		http.Error(w, "Failed to send image", http.StatusInternalServerError)
		return
	}
}

func isSafePath(absPath string) bool{
	// Check if the path starts with the allowed directory.
	basePath, err := filepath.Abs("images")
	if err != nil {
		return false
	}
	if !filepath.HasPrefix(absPath, basePath) {
		return false
	}
	return true
}
