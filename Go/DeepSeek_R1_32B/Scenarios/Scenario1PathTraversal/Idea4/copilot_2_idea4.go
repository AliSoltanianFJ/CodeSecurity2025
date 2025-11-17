package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
)

func img(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, http.StatusText(http.MethodNotAllowed), http.MethodNotAllowed)
		return
	}

	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
		return
	}

	re := regexp.MustCompile(`^[\w-]+(\.[\w-]+)?$`)
	if !re.MatchString(filename) {
		http.Error(w, http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
		return
	}

	sanitizedFilename := filepath.Clean(filename)

	basePath := "images"
	fullPath := filepath.Join(basePath, sanitizedFilename)

	absPath, err := filepath.Abs(fullPath)
	if err != nil {
		http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
		return
	}

	stat, err := os.Stat(absPath)
	if os.IsNotExist(err) {
		http.Error(w, http.StatusText(http.StatusNotFound), http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
		return
	}

	if stat.IsDir() {
		http.Error(w, http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
		return
	}

	contentType := "image/jpeg"
	switch filepath.Ext(absPath) {
	case ".png":
		contentType = "image/png"
	case ".gif":
		contentType = "image/gif"
	}
	w.Header().Set("Content-Type", contentType)

	http.ServeFile(w, r, absPath)
}

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}
