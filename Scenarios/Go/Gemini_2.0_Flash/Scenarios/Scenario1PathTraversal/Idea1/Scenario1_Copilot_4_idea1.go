package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"log"
)

func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	// Path traversal protection
	cleanFilename := filepath.Clean(filename)
	if strings.Contains(cleanFilename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	filePath := filepath.Join("assets", "images", cleanFilename)

	// Verify file existence
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, filePath)
}

func main() {
	http.HandleFunc("/image/", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}