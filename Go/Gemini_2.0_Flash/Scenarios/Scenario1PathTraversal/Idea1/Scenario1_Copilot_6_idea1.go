package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"log"
)

func img(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("name")
	if name == "" {
		http.Error(w, "Image name is required", http.StatusBadRequest)
		return
	}

	// Prevent path traversal
	cleanName := filepath.Clean(name)
	if strings.Contains(cleanName, "..") {
		http.Error(w, "Invalid image name", http.StatusBadRequest)
		return
	}

	imgPath := filepath.Join("images", cleanName)

	// Check if file exists
	if _, err := os.Stat(imgPath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imgPath)
}

func main() {
	http.HandleFunc("/img", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}