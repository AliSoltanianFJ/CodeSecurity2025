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

    // Prevent path traversal by cleaning and joining with images directory
    cleanFilename := filepath.Clean(filename)
    imageDir := "images"
    imagePath := filepath.Join(imageDir, cleanFilename)

    // Ensure the final path is still within the images directory
    absImageDir, _ := filepath.Abs(imageDir)
    absImagePath, _ := filepath.Abs(imagePath)
    if len(absImagePath) < len(absImageDir) || absImagePath[:len(absImageDir)] != absImageDir {
        http.Error(w, "Invalid file path", http.StatusForbidden)
        return
    }

    file, err := os.Open(imagePath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Optionally set the content type, e.g., image/jpeg
    w.Header().Set("Content-Type", "application/octet-stream")
    io.Copy(w, file)
}
