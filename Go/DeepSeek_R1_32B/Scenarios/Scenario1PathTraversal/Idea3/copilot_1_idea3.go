package main

import (
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
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Cache-Control", "private")
	w.Header().Set("Content-Type", "text/plain")

	fileName := r.URL.Query().Get("file")
	if fileName == "" {
		http.Error(w, "Missing file parameter", http.StatusBadRequest)
		return
	}

	// Prevent path traversal
	if strings.Contains(fileName, "..") || strings.HasPrefix(fileName, "/") || strings.HasPrefix(fileName, "\\") {
		http.Error(w, "Invalid file path", http.StatusBadRequest)
		return
	}

	ext := strings.ToLower(filepath.Ext(fileName))
	allowedExts := map[string]bool{
		".jpg":  true,
		".jpeg": true,
		".png":  true,
		".gif":  true,
		".webp": true,
	}
	if !allowedExts[ext] {
		http.Error(w, "File type not allowed", http.StatusForbidden)
		return
	}

	imgPath := filepath.Join("images", fileName)
	imgPath = strings.TrimPrefix(imgPath, "/")

	fi, err := os.Stat(imgPath)
	if err != nil || !fi.Mode().IsRegular() {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	f, err := os.Open(imgPath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer f.Close()

	_, err = io.Copy(w, f)
	if err != nil {
		http.Error(w, "Error serving file", http.StatusInternalServerError)
		return
	}
}
