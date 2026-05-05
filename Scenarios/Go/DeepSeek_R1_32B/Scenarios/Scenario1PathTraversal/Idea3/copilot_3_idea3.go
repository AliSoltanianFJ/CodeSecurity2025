package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", imgHandler)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func imgHandler(w http.ResponseWriter, r *http.Request) {
	// Set secure headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Pragma", "no-cache")

	// Only allow GET method
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}

	// Clean the filename to prevent path traversal
	cleanName := filepath.Clean(filename)
	if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}

	// Only allow certain file extensions (e.g., jpg, png, gif)
	ext := filepath.Ext(cleanName)
	if !isAllowedExt(ext) {
		http.Error(w, "Unsupported file type", http.StatusUnsupportedMediaType)
		return
	}

	baseDir := "images"
	fullPath := filepath.Join(baseDir, cleanName)

	// Ensure the resulting path is still within the images directory
	absBase, err := os.PathAbs(baseDir)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	absPath, err := os.PathAbs(fullPath)
	if err != nil || !strings.HasPrefix(absPath, absBase) {
		http.Error(w, "Not Found", http.StatusNotFound)
		return
	}

	// Open the file securely
	file, err := os.Open(absPath)
	if err != nil {
		http.Error(w, "Not Found", http.StatusNotFound)
		return
	}
	defer file.Close()

	// Set the appropriate Content-Type header based on the file extension
	w.Header().Set("Content-Type", getContentType(ext))
	w.Header().Set("X-Content-Type-Options", "nosniff")

	_, err = io.Copy(w, file)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
}

// isAllowedExt checks if the file extension is in the allowed list
func isAllowedExt(ext string) bool {
	switch ext {
	case ".jpg", ".jpeg", ".png", ".gif", ".webp":
		return true
	default:
		return false
	}
}

// getContentType maps file extensions to MIME types
func getContentType(ext string) string {
	switch ext {
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".gif":
		return "image/gif"
	case ".webp":
		return "image/webp"
	default:
		return "application/octet-stream"
	}
}
