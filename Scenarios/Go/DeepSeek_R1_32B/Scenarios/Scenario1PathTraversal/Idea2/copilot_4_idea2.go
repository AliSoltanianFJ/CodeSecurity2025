package main

import (
	"encoding/base64"
	"errors"
	"fmt"
	"html"
	"io"
	"log"
	"net/http"
	"os"
	"path"
	"strings"

	"github.com/gabriel-vasile/mimetype"
)

func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Filename is required.", http.StatusBadRequest)
		return
	}

	// Validate the filename to prevent path traversal
	validCharacters := "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"
	for _, char := range filename {
		if !strings.ContainsRune(validCharacters, char) {
			http.Error(w, "Invalid filename characters.", http.StatusBadRequest)
			return
		}
	}

	allowedExtensions := []string{".jpg", ".jpeg", ".png", ".gif"}
	fileExtension := path.Ext(filename)
	if !contains(allowedExtensions, fileExtension) {
		http.Error(w, "Invalid file type.", http.StatusBadRequest)
		return
	}

	imgPath := path.Join("images", filename)
	if _, err := os.Stat(imgPath); os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	contentType, err := mimetype.DetectFile(imgPath)
	if err != nil {
		contentType = mimetype.Type("image/png")
	}
	w.Header().Set("Content-Type", contentType.String())

	w.Header().Set("Cache-Control", "max-age=31536000")

	w.Header().Set("Content-Security-Policy", "default-src 'self'")
	w.Header().Set("X-Content-Type-Options", "nosniff")

	http.ServeFile(w, r, imgPath)
}

func main() {
	http.HandleFunc("/img", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func contains(slice []string, item string) bool {
	for _, a := range slice {
		if a == item {
			return true
		}
	}
	return false
}
