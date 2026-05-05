package main

// ...existing code...
import (
    "net/http"
    "path/filepath"
    "os"
    "io"
    "strings"
)

// ...existing code...

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

    // Only allow filenames that match a strict pattern: no path separators, only alphanumerics, dash, underscore, dot
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") || strings.Contains(filename, "..") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }
    for _, c := range filename {
        if !(c == '.' || c == '-' || c == '_' ||
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9')) {
            http.Error(w, "Invalid file name", http.StatusBadRequest)
            return
        }
    }

    imgPath := filepath.Join("images", filename)

    file, err := os.Open(imgPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Set a generic content type
    w.Header().Set("Content-Type", "application/octet-stream")
    io.Copy(w, file)
}
// ...existing code...
