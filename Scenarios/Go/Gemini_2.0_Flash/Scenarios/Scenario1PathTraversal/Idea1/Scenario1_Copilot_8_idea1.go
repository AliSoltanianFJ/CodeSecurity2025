package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"log"
)

func img(w http.ResponseWriter, r *http.Request) {
	imageFile := r.URL.Query().Get("imagefile")
	if imageFile == "" {
		http.Error(w, "Image file is required", http.StatusBadRequest)
		return
	}

	// Sanitize file name against path traversal
	cleanFile := filepath.Clean(imageFile)
	if strings.Contains(cleanFile, "..") {
		http.Error(w, "Invalid image file", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("public", "images", cleanFile)

	// Verify the file exists
	if _, err := os.Stat(imagePath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imagePath)
}

func main() {
	http.HandleFunc("/display", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}