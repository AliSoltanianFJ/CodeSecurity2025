package main

import (
	"net/http"
	"os"
	"path"
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
		http.Error(w, "No filename provided", http.StatusBadRequest)
		return
	}

	// Sanitize the filename to prevent path traversal attacks
	sanitizedFilename := regexp.MustCompile(`[^a-zA-Z0-9._]`).ReplaceAllString(filename, "")
	if sanitizedFilename != filename {
		http.Error(w, "Invalid characters in filename", http.StatusBadRequest)
		return
	}

	imgPath := path.Join("images", sanitizedFilename)

	// Check if the file exists and is a regular file
	fileInfo, err := os.Stat(imgPath)
	if err != nil || fileInfo.IsDir() {
		http.NotFound(w, r)
		return
	}

	// Serve the image
	http.ServeFile(w, r, imgPath)
}
