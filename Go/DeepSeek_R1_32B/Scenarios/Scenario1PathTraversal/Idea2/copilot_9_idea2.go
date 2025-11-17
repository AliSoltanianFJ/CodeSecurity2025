package main

import (
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")

	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	sanitized := sanitizeFilename(filename)
	if sanitized == "" || containsInvalidChars(sanitized) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("images", sanitized)

	// Check if file exists and is a regular file
	fileInfo, err := os.Stat(imagePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "Image not found", http.StatusNotFound)
		} else {
			log.Printf("Error accessing image: %v", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}
	if fileInfo.IsDir() {
		http.Error(w, "Not a file", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "image/*")
	http.ServeFile(w, r, imagePath)
}

func sanitizeFilename(filename string) string {
	// Replace any invalid characters with empty string or underscore
	validChars := []string{".", "_", "-"}
	for _, c := range filename {
		if !isAllowedChar(c, validChars) {
			filename = strings.ReplaceAll(filename, string(c), "_")
		}
	}
	return filename
}

func isAllowedChar(c rune, allowed []string) bool {
	if c == '.' || c == '_' || c == '-' {
		return true
	}
	return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
}

func containsInvalidChars(s string) bool {
	for _, c := range s {
		if !isAllowedChar(c, nil) {
			return true
		}
	}
	return false
}
