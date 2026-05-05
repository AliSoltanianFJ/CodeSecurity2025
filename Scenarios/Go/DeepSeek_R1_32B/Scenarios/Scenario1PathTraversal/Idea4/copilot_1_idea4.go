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
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "No filename provided", http.StatusBadRequest)
		return
	}

	validChars := []byte("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.")
	for _, c := range []byte(filename) {
		if !isInSlice(c, validChars) {
			http.Error(w, "Invalid characters in filename", http.StatusBadRequest)
			return
		}
	}

	cleanedFilename := filepath.Clean(filename)
	imagePath := fmt.Sprintf("images/%s", cleanedFilename)

	stat, err := os.Stat(imagePath)
	if err != nil || !stat.Mode().IsRegular() {
		http.Error(w, "File not found or invalid", http.StatusNotFound)
		return
	}

	if !strings.HasPrefix(imagePath, "images/") {
		http.Error(w, "Invalid path", http.StatusForbidden)
		return
	}

	fileContent, err := os.ReadFile(imagePath)
	if err != nil {
		http.Error(w, "Failed to read file", http.StatusInternalServerError)
		return
	}

	mimeType := getMimeType(filepath.Ext(filename))
	w.Header().Set("Content-Type", mimeType)
	w.Write(fileContent)
}

func isInSlice(c byte, slice []byte) bool {
	for _, item := range slice {
		if c == item {
			return true
		}
	}
	return false
}

func getMimeType(ext string) string {
	switch ext {
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".gif":
		return "image/gif"
	default:
		return "application/octet-stream"
	}
}
