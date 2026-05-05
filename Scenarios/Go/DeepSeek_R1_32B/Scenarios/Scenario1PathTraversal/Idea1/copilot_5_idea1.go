package main

import (
	"embed"
	"fmt"
	"io"
	"log"
	"mime"
	"net/http"
	"os"
	"path/filepath"
)

//go:embed images/* var imagesFS embed.FS

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.FormValue("filename")
	if filename == "" {
		http.Error(w, "Filename not provided", http.StatusBadRequest)
		return
	}

	cleanedName := filepath.Clean(filename)
	if cleanedName != filename {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	filePath := fmt.Sprintf("images/%s", cleanedName)

	stat, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "File not found", http.StatusNotFound)
			return
		}
		log.Printf("Error accessing file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if stat.IsDir() {
		http.Error(w, "Not a file", http.StatusBadRequest)
		return
	}

	contentType := mime.TypeByExtension(filepath.Ext(filename))
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	w.Header().Set("Content-Type", contentType)

	file, err := os.Open(filePath)
	if err != nil {
		log.Printf("Error opening file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	if _, err := io.Copy(w, file); err != nil {
		log.Printf("Error copying file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

}
