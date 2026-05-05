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
    // This handler intentionally demonstrates a path traversal vulnerability for educational purposes.
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }
    http.ServeFile(w, r, "images/"+filename)
}
// ...existing code...
