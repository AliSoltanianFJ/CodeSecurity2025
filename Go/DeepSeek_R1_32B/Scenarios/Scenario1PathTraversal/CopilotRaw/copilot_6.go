package main

import (
    "fmt"
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
        http.Error(w, "filename parameter is required", http.StatusBadRequest)
        return
    }

    filePath := filepath.Join("images", filename)

    // Check if the file exists
    if _, err := os.Stat(filePath); os.IsNotExist(err) {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // Read the image file
    data, err := os.ReadFile(filePath)
    if err != nil {
        http.Error(w, "failed to read file", http.StatusInternalServerError)
        return
    }

    // Determine the content type
    contentType := http.DetectContentType(data)

    // Set headers and send response
    w.Header().Set("Content-Type", contentType)
    w.Write(data)
}