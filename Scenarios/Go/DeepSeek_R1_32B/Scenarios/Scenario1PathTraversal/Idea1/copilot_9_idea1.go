package main

import (
	"io"
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
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "No filename provided", http.StatusBadRequest)
		return
	}

	cleanedFilename := filepath.Clean(filename)

	basePath, err := os.DirFS("images").EnsureDir()
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	baseAbsPath, err := filepath.Abs(basePath.String())
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fileAbsPath := filepath.Join(baseAbsPath, cleanedFilename)

	baseParent, err := filepath.Abs(filepath.Dir(fileAbsPath))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if baseParent != baseAbsPath {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	file, err := os.Open(fileAbsPath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "File not found", http.StatusNotFound)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}
	defer file.Close()

	ext := filepath.Ext(cleanedFilename)
	contentType := mime.TypeByExtension(ext[1:])
	if contentType == "" {
		contentType = "application/octet-stream"
	}

	http.ServeContent(w, r, cleanedFilename, file.Header().Get("Last-Modified"), file)
}
