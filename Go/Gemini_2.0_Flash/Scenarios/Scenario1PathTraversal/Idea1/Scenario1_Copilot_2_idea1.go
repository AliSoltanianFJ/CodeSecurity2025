package main

import (
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func img(w http.ResponseWriter, r *http.Request) {
	imgName := r.URL.Query().Get("name")
	if imgName == "" {
		http.Error(w, "Image name is required", http.StatusBadRequest)
		return
	}

	//  Path traversal prevention
	cleanName := filepath.Clean(imgName)
	if strings.Contains(cleanName, "..") {
		http.Error(w, "Invalid image name", http.StatusBadRequest)
		return
	}

	imgPath := filepath.Join("public", "images", cleanName)

	// Check if image exists
	_, err := os.Stat(imgPath)
	if os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imgPath)
}

func main() {
	http.HandleFunc("/image", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}
