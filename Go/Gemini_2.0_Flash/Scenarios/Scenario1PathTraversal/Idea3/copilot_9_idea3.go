package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Filename not provided", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("images", filename)

	// Security check: prevent path traversal attacks
	absPath, err := filepath.Abs(imagePath)
	if err != nil {
		http.Error(w, "Invalid path", http.StatusBadRequest)
		return
	}

	if !isValidPath(absPath) {
		http.Error(w, "Invalid image path", http.StatusBadRequest)
		return
	}

	// Open the image file
	file, err := os.Open(absPath)
	if err != nil {
		fmt.Println(err)
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}
	defer file.Close()

	// Serve the image
	http.ServeContent(w, r, filename, time.Now(), file)
}

// is_valid_path function is necessary to avoid directory traversal attacks.
func isValidPath(absPath string) bool {
	// Check if the path contains directory traversal sequences (e.g., "..")
	if strings.Contains(absPath, "..") {
		return false
	}
	return true
}