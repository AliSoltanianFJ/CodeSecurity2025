package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	validName := regexp.MustCompile(`^[a-zA-Z0-9_\-\.]+$`)
	if !validName.MatchString(filename) {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	baseDir := "images"
	sanitizedFilename := filepath.Clean(filename)
	imagePath := filepath.Join(baseDir, sanitizedFilename)

	// Check if file exists and is not a directory
	stat, err := os.Stat(imagePath)
	if err != nil {
		if os.IsNotExist(err) {
			w.WriteHeader(http.StatusNotFound)
		} else {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Error accessing image: %v", err)
		}
		return
	}

	if stat.IsDir() {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	// Set headers to prevent caching and specify content type
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Content-Disposition", "attachment; filename="+filename)

	http.ServeFile(w, r, imagePath)
}
