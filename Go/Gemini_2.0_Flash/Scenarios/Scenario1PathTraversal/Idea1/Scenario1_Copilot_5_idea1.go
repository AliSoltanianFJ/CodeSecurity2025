package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"fmt"
)

func img(w http.ResponseWriter, r *http.Request) {
	imageFile := r.URL.Query().Get("file")
	if imageFile == "" {
		http.Error(w, "Image file name is required", http.StatusBadRequest)
		return
	}

	// Path traversal protection
	imageFile = filepath.Clean(imageFile)
	if strings.Contains(imageFile, "..") {
		http.Error(w, "Invalid image file name", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("static", "images", imageFile)

	// Check if the file exists
	if _, err := os.Stat(imagePath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imagePath)
}

func main() {
	http.HandleFunc("/images", img)
    fmt.Println("Server listening on port 8080")
	http.ListenAndServe(":8080", nil)
}