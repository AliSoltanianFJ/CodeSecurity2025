package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
)

func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Filename not provided", http.StatusBadRequest)
		return
	}

	sanitizedName := filepath.Base(filename)
	filePath := fmt.Sprintf("images/%s", sanitizedName)

	_, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "File not found", http.StatusNotFound)
			return
		}
		log.Printf("Error accessing file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fileContent, err := os.ReadFile(filePath)
	if err != nil {
		log.Printf("Error reading file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	ext := filepath.Ext(sanitizedName)
	mimeType := mime.TypeByExtension(ext[1:])
	if mimeType == "" {
		mimeType = "application/octet-stream"
	}
	w.Header().Set("Content-Type", mimeType)

	io.Copy(w, fileContent)
}

func main() {
	http.HandleFunc("/img", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}
