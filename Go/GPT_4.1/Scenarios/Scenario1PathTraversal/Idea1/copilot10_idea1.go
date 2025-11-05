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
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Only allow filenames that match a strict pattern: no path separators, no traversal
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") || strings.Contains(filename, "..") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Optionally, restrict to certain extensions (e.g., jpg, png, gif)
    ext := strings.ToLower(filepath.Ext(filename))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif":
        // allowed
    default:
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    imgPath := filepath.Join("images", filename)
    f, err := os.Open(imgPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    io.Copy(w, f)
}
// ...existing code...
