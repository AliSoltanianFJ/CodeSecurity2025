package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func img(w http.ResponseWriter, r *http.Request) {
	imageName := r.URL.Query().Get("name")
	if imageName == "" {
		http.Error(w, "Image name is required", http.StatusBadRequest)
		return
	}

	// Sanitize the image name to prevent path traversal
	imageName = filepath.Clean(imageName)
	if strings.Contains(imageName, "..") {
		http.Error(w, "Invalid image name", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("images", imageName) // Assuming "images" folder is in the same directory

	// Check if the file exists
	if _, err := os.Stat(imagePath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	// Serve the image
	http.ServeFile(w, r, imagePath)
}

func main() {
	http.HandleFunc("/img", img)
	fmt.Println("Server listening on port 8080")
	http.ListenAndServe(":8080", nil)
}
