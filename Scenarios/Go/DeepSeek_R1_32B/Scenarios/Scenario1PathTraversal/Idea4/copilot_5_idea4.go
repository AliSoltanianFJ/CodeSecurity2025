package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
)

func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	// Validate and sanitize the filename to prevent path traversal attacks
	valid := regexp.MustCompile(`^[a-zA-Z0-9_\-\.]+$`)
	if !valid.MatchString(filename) {
		http.Error(w, "Invalid filename characters", http.StatusBadRequest)
		return
	}

	// Construct the full file path safely
	filePath := filepath.Join("images", filename)

	// Check if the file exists and is a regular file (not directory or symlink)
	info, err := os.Stat(filePath)
	if err != nil {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	if info.IsDir() || !info.Mode().IsRegular() {
		http.Error(w, "Invalid file type", http.StatusBadRequest)
		return
	}

	// Serve the file with appropriate content type
	http.ServeFile(w, r, filePath)
}

func main() {
	http.HandleFunc("/img", img)
	log.Println("Starting server on :8080")
	if err := http.ListenAndServe(":8080", nil); err != nil {
		log.Fatal(err)
	}
}
