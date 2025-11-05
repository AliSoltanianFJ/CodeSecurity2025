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

    // Prevent path traversal by cleaning and joining the path
    cleanFilename := filepath.Base(filename)
    imagePath := filepath.Join("images", cleanFilename)

    file, err := os.Open(imagePath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Optionally set the content type
    w.Header().Set("Content-Type", "image/jpeg")
    io.Copy(w, file)
}
