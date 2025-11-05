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
    // This implementation intentionally does not sanitize or validate the file parameter.
    // It demonstrates a path traversal vulnerability for educational purposes.
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    imagePath := "images/" + filename

    http.ServeFile(w, r, imagePath)
}
// ...existing code...
