package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func img(w http.ResponseWriter, r *http.Request) {
	imageID := r.URL.Query().Get("id")
	if imageID == "" {
		http.Error(w, "Image ID is required", http.StatusBadRequest)
		return
	}

	//  Path traversal vulnerability mitigation
	cleanID := filepath.Clean(imageID)
	if strings.Contains(cleanID, "..") {
		http.Error(w, "Invalid image ID", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("assets", cleanID)

	// Check if file exists
	if _, err := os.Stat(imagePath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imagePath)
}

func main() {
	http.HandleFunc("/showImage", img)
	fmt.Println("Server listening on port 8080")
	http.ListenAndServe(":8080", nil)
}