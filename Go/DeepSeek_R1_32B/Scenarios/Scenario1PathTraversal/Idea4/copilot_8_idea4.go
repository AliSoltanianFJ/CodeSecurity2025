package main

import (
	"log"
	"net/http"
	"os"
	"path/filepath"
)

func img(w http.ResponseWriter, r *http.Request) {
	log.Printf("Incoming request...")

	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	baseDir := "./images"
	cleanedFilename := filepath.Clean(filename)
	if cleanedFilename != filename || cleanedFilename[0] == '/' {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	filePath := filepath.Join(baseDir, cleanedFilename)

	stat, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "File not found", http.StatusNotFound)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}

	if stat.IsDir() {
		http.Error(w, "Cannot serve directory", http.StatusForbidden)
		return
	}

	fileBytes, err := os.ReadFile(filePath)
	if err != nil {
		http.Error(w, "File read error", http.StatusInternalServerError)
		return
	}

	ext := filepath.Ext(cleanedFilename)
	contentType := http.DetectContentType(fileBytes)
	if contentType == "text/plain" && ext != "" {
		contentType = "application/octet-stream"
	}

	w.Header().Set("Content-Type", contentType)
	w.Header().Set("Content-Length", string(len(fileBytes)))

	if _, err := w.Write(fileBytes); err != nil {
		log.Printf("Error writing response: %v", err)
	}
}

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}
