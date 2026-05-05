package main

import (
	"encoding/hex"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	file := r.URL.Query().Get("file")
	if file == "" {
		http.Error(w, "No file specified", http.StatusBadRequest)
		return
	}

	sanitizedFile, err := filepath.Abs(filepath.Clean(file))
	if err != nil {
		http.Error(w, "Invalid file path", http.StatusBadRequest)
		return
	}

	baseDir := "images"
	basePath, err := filepath.Abs(baseDir)
	if err != nil {
		http.Error(w, "Server error", http.StatusInternalServerError)
		return
	}

	filePath := filepath.Join(basePath, sanitizedFile)

	stat, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	if err != nil {
		http.Error(w, "Server error", http.StatusInternalServerError)
		return
	}

	if stat.IsDir() {
		http.Error(w, "Not a file", http.StatusBadRequest)
		return
	}

	f, err := os.Open(filePath)
	if err != nil {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	defer f.Close()

	content, err := ioutil.ReadAll(f)
	if err != nil {
		http.Error(w, "Server error", http.StatusInternalServerError)
		return
	}

	mimeType := http.DetectContentType(content)

	w.Header().Set("Content-Type", mimeType)
	w.WriteHeader(http.StatusOK)
	w.Write(content)
}

func isValidFilename(filename string) bool {
	// Check if the filename contains only allowed characters
	for _, char := range filename {
		if !isAllowedChar(char) {
			return false
		}
	}
	return true
}

func isAllowedChar(char rune) bool {
	// Allow alphanumeric characters, dot, underscore, and hyphen
	return char >= 'a' && char <= 'z' ||
		char >= 'A' && char <= 'Z' ||
		char >= '0' && char <= '9' ||
		char == '.' || char == '_' || char == '-'
}
