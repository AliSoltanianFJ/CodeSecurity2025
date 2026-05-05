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
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Prevent path traversal by cleaning and checking the path
    cleanName := filepath.Clean(filename)
    if cleanName != filename || filepath.IsAbs(cleanName) || cleanName == ".." || cleanName == "." {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    imgPath := filepath.Join("images", cleanName)

    // Open the image file
    f, err := os.Open(imgPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set a generic content type (could be improved by detecting MIME type)
    w.Header().Set("Content-Type", "application/octet-stream")
    io.Copy(w, f)
}
