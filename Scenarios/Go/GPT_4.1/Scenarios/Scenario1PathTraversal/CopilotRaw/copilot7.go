package main

import (
    "net/http"
    "path/filepath"
    "os"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Prevent path traversal
    cleanFilename := filepath.Clean(filename)
    if strings.Contains(cleanFilename, "..") || strings.HasPrefix(cleanFilename, "/") || strings.HasPrefix(cleanFilename, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    imgPath := filepath.Join("images", cleanFilename)

    // Check if file exists and is not a directory
    info, err := os.Stat(imgPath)
	if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    http.ServeFile(w, r, imgPath)
}
