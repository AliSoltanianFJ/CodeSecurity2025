package main

// ...existing code...
import (
    "net/http"
    "path/filepath"
    "os"
    "io"
    "strings"
    "mime"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}
// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow a fixed set of image files to be served
    allowedImages := map[string]string{
        "cat.jpg":  "images/cat.jpg",
        "dog.png":  "images/dog.png",
        "bird.gif": "images/bird.gif",
    }

    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    imagePath, ok := allowedImages[filename]
    if !ok {
        http.Error(w, "File not allowed", http.StatusForbidden)
        return
    }

    http.ServeFile(w, r, imagePath)
}
// ...existing code...
