package main

import (
	"bytes"
	"embed"
	"fmt"
	"io"
	"net/http"
	"os"
)

//go:embed images/*
var assets embed.FS

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

	stat, err := assets.Stat(fmt.Sprintf("images/%s", filename))
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "File not found", http.StatusNotFound)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}

	if stat.IsDir() {
		http.Error(w, "Not a file", http.StatusBadRequest)
		return
	}

	content, err := assets.ReadFile(fmt.Sprintf("images/%s", filename))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	contentType := http.DetectContentType(content)
	w.Header().Set("Content-Type", contentType)

	io.Copy(w, bytes.NewReader(content))
}
