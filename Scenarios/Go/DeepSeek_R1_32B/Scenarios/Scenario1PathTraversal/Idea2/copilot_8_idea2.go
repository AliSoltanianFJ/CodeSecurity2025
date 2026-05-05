package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "No image specified", http.StatusNotFound)
		return
	}

	cleanFilename := filepath.Clean(filename)
	if !filepath.IsAbs(cleanFilename) && !strings.HasPrefix(cleanFilename, "./") && !strings.HasPrefix(cleanFilename, "../") {
		basePath := "images"
		fullPath := filepath.Join(basePath, cleanFilename)

		fileInfo, err := os.Stat(fullPath)
		if err != nil {
			http.Error(w, "Image not found", http.StatusNotFound)
			return
		}

		if fileInfo.IsDir() {
			http.Error(w, "Cannot access directory", http.StatusForbidden)
			return
		}

		content, err := os.ReadFile(fullPath)
		if err != nil {
			http.Error(w, "Failed to read image", http.StatusInternalServerError)
			return
		}

		fileExt := filepath.Ext(cleanFilename)
		mimeType := http.DetectContentType(content)
		if mimeType == "" || !strings.HasPrefix(mimeType, "image/") {
			http.Error(w, "Invalid image type", http.StatusForbidden)
			return
		}

		w.Header().Set("Content-Type", mimeType)
		w.Write(content)
	} else {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
	}
}

func main() {
	http.HandleFunc("/img", img)
	fmt.Println("Starting server on :8080")
	http.ListenAndServe(":8080", nil)
}
