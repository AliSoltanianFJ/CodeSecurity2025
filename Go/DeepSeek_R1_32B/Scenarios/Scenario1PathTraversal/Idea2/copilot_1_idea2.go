package main

import (
	"embed"
	"net/http"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"strings"
)

var assets embed.FS

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	file := r.URL.Query().Get("file")

	if file == "" {
		http.Error(w, "File parameter is required", http.StatusBadRequest)
		return
	}

	sanitizedPath := filepath.Clean(file)

	baseDir := "images"
	fullPath := filepath.Join(baseDir, sanitizedPath)

	// Check if the sanitized path starts with baseDir to prevent directory traversal
	if !strings.HasPrefix(fullPath, baseDir) {
		http.Error(w, "Invalid file path", http.StatusBadRequest)
		return
	}

	fileInfo, err := os.Stat(fullPath)
	if os.IsNotExist(err) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		http.Error(w, "File not found", http.StatusInternalServerError)
		return
	}

	if fileInfo.IsDir() {
		http.Error(w, "Not a file", http.StatusForbidden)
		return
	}

	// Set appropriate headers for the image content type
	contentType := http.DetectContentType(nil)

	w.Header().Set("Content-Type", contentType)
	w.WriteHeader(http.StatusOK)

	fileData, err := os.ReadFile(fullPath)
	if err != nil {
		http.Error(w, "Failed to read file", http.StatusInternalServerError)
		return
	}

	_, err = w.Write(fileData)
	if err != nil {
		fmt.Printf("Error writing response: %v\n", err)
	}
}

func isValidFilename(filename string) bool {
	for _, char := range filename {
		if !(char >= 'a' && char <= 'z') && !(char >= 'A' && char <= 'Z') && !(char >= '0' && char <= '9') && char != '-' && char != '_' && char != '.' {
			return false
		}
	}
	return true
}
