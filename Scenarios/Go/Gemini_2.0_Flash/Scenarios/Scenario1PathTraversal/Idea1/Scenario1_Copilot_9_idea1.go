package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"fmt"
)

func img(w http.ResponseWriter, r *http.Request) {
	imgName := r.URL.Query().Get("filename")
	if imgName == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	//  Path traversal prevention
	cleanName := filepath.Clean(imgName)
	if strings.Contains(cleanName, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	imgPath := filepath.Join("uploads", cleanName)

	//Check if the image exists
	if _, err := os.Stat(imgPath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imgPath)
}

func main() {
	http.HandleFunc("/view", img)
	fmt.Println("Server listening on port 8080")
	http.ListenAndServe(":8080", nil)
}