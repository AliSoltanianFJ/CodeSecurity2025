package main

import (
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	filename := query.Get("filename")

	if filename == "" {
		http.Error(w, "No filename provided", http.StatusBadRequest)
		return
	}

	// Validate the filename to prevent path traversal attacks
	validChars := setValidCharacters([]byte("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-."))

	for _, c := range []byte(filename) {
		if !validChars[c] {
			http.Error(w, "Invalid characters in filename", http.StatusBadRequest)
			return
		}
	}

	// Check for allowed extensions
	ext := filepath.Ext(filename)
	switch ext {
	case ".jpg", ".jpeg", ".png", ".gif", ".webp":
		// Valid extension
	default:
		http.Error(w, "Invalid file type", http.StatusBadRequest)
		return
	}

	imgPath := filepath.Join("images", filename)
	canonicalPath, err := filepath.Abs(imgPath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if !strings.HasPrefix(canonicalPath, os.Getenv("IMAGE_ROOT")) {
		http.Error(w, "Invalid file path", http.StatusBadRequest)
		return
	}

	file, err := os.Open(imgPath)
	if err != nil {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	defer file.Close()

	stat, err := file.Stat()
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", http.DetectContentType(nil))
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="%s"`, filename))
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("Content-Security-Policy", `default-src 'self'`)

	if _, err := io.Copy(w, file); err != nil {
		http.Error(w, "Error serving file", http.StatusInternalServerError)
		return
	}
}

func setValidCharacters(valid []byte) map[byte]bool {
	m := make(map[byte]bool)
	for _, c := range valid {
		m[c] = true
	}
	return m
}
