package main

import (
	"io"
	"mime"
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
	filename := r.FormValue("filename")
	if filename == "" {
		http.Error(w, "Filename not provided", http.StatusBadRequest)
		return
	}

	baseName := filepath.Base(filename)
	ext := filepath.Ext(baseName)
	if ext != "" && !isValidExtension(ext) {
		http.Error(w, "Invalid file extension", http.StatusBadRequest)
		return
	}

	filePath := filepath.Join("images", baseName)

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	data, err := os.ReadFile(filePath)
	if err != nil {
		http.Error(w, "Error reading file", http.StatusInternalServerError)
		return
	}

	mimeType := mime.TypeByExtension(ext[1:])
	if mimeType == "" {
		mimeType = "application/octet-stream"
	}
	w.Header().Set("Content-Type", mimeType)

	io.Copy(w, data)
}

func isValidExtension(ext string) bool {
	validExtensions := map[string]bool{
		".png":  true,
		".jpg":  true,
		".jpeg": true,
		".gif":  true,
	}

	return validExtensions[ext]
}
