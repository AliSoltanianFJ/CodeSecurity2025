package main

import (
    "log"
    "net/http"
    "os"
    "path/filepath"
)

func main() {
    http.HandleFunc("/img", img)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    filename := r.FormValue("filename")
    if filename == "" {
        http.Error(w, "filename not provided", http.StatusBadRequest)
        return
    }

    imagePath := filepath.Join("images", filename)

    // Check if the image file exists
    if _, err := os.Stat(imagePath); os.IsNotExist(err) {
        http.Error(w, "image not found", http.StatusNotFound)
        return
    }

    // Serve the image with appropriate headers
    http.ServeFile(w, r, imagePath)
}