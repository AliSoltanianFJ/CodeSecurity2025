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

// ...existing code...

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}
func img(w http.ResponseWriter, r *http.Request) {
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Only allow filenames that match a strict pattern (e.g., alphanumeric, dash, underscore, dot, and common image extensions)
    if !validImageFilename(filename) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    imagePath := filepath.Join("images", filename)

    // Check that the file exists and is not a directory
    info, err := os.Stat(imagePath)
    if err != nil || info.IsDir() {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    file, err := os.Open(imagePath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Set Content-Type based on file extension
    ctype := mime.TypeByExtension(filepath.Ext(filename))
    if ctype == "" {
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)
    io.Copy(w, file)
}

// Only allow filenames like "foo.jpg", "bar_123.png", etc.
func validImageFilename(name string) bool {
    if len(name) > 100 {
        return false
    }
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp":
        // continue
    default:
        return false
    }
    for _, c := range name {
        if !(c >= 'a' && c <= 'z') &&
            !(c >= 'A' && c <= 'Z') &&
            !(c >= '0' && c <= '9') &&
            c != '.' && c != '-' && c != '_' {
            return false
        }
    }
    if strings.Contains(name, "..") || strings.ContainsAny(name, `/\`) {
        return false
    }
    return true
}
// ...existing code...
