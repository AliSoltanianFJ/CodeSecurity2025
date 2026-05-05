package main

import (
	"fmt"
	"html"
	"path/filepath"
	"net/http"
	"path"
	"regexp"
	"strings"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	fileName := r.URL.Query().Get("filename")

	// Check if filename is provided
	if fileName == "" {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Use a regular expression to validate the filename
	valid := regexp.MustCompile(`^[a-zA-Z0-9_\-\.]+$`)
	if !valid.MatchString(fileName) {
		http.Error(w, "Invalid characters in filename", http.StatusBadRequest)
		return
	}

	// Sanitize the filename by escaping HTML characters
	sanitizedFilename := html.EscapeString(fileName)

	// Define allowed file extensions
	allowedExtensions := []string{".png", ".jpg", ".jpeg", ".gif"}
	ext := path.Ext(sanitizedFilename)
	if !contains(allowedExtensions, strings.ToLower(ext)) {
		http.Error(w, "Invalid file extension", http.StatusBadRequest)
		return
	}

	// Construct the full file path safely
	imagePath := filepath.Join("images", sanitizedFilename)

	if _, err := filepath.Abs(imagePath); err != nil {
		http.Error(w, "Invalid path", http.StatusInternalServerError)
		return
	}

	// Check if the file exists and is within the intended directory
	if !fileExists(imagePath) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	// Set security headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Security-Policy", "default-src 'self'")

	// Serve the image file
	http.ServeFile(w, r, imagePath)
}

func contains(slice []string, item string) bool {
	for _, a := range slice {
		if a == item {
			return true
		}
	}
	return false
}

func fileExists(filePath string) bool {
	_, err := os.Stat(filePath)
	return !os.IsNotExist(err)
}
