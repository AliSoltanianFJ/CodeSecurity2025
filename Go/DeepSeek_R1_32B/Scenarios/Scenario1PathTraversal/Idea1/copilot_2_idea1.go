package main

import (
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
	filename := r.FormValue("filename")
	if filename == "" {
		http.Error(w, "Filename not provided", http.StatusBadRequest)
		return
	}

	sanitized := sanitizeFilename(filename)
	if sanitized == "" {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	baseDir := "images"
	filePath := filepath.Join(baseDir, sanitized)

	// Check if the file exists and is a regular file
	stat, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "File not found", http.StatusNotFound)
		} else {
			http.Error(w, "Error accessing file", http.StatusInternalServerError)
		}
		return
	}

	if !stat.Mode().IsRegular() {
		http.Error(w, "Not a regular file", http.StatusForbidden)
		return
	}

	// Ensure the resolved path is within the base directory
	baseCanonical, err := filepath.Abs(baseDir)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fileCanonical, err := filepath.Abs(filePath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if !strings.HasPrefix(fileCanonical, baseCanonical) {
		http.Error(w, "Access denied", http.StatusForbidden)
		return
	}

	http.ServeFile(w, r, filePath)
}

func sanitizeFilename(filename string) string {
	// Remove any path traversal characters
	filename = filepath.Base(filename)

	// Optionally, you can add more sanitization rules here

	return filename
}
