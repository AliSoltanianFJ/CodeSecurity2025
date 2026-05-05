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

    // Only allow filenames that match a strict pattern (e.g., alphanumeric, dash, underscore, dot, and common image extensions)
    if !isValidImageFilename(filename) {
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

    // Open the file
    file, err := os.Open(imagePath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Set the Content-Type header based on the file extension
    ctype := mime.TypeByExtension(filepath.Ext(filename))
    if ctype == "" {
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)

    io.Copy(w, file)
}

// isValidImageFilename checks if the filename is a valid image file name (no path separators, only allowed chars, and valid extension)
func isValidImageFilename(name string) bool {
    if strings.Contains(name, "/") || strings.Contains(name, "\\") {
        return false
    }
    allowedExts := map[string]bool{
        ".jpg": true, ".jpeg": true, ".png": true, ".gif": true, ".bmp": true, ".webp": true,
    }
    ext := strings.ToLower(filepath.Ext(name))
    if !allowedExts[ext] {
        return false
    }
    for _, c := range name {
        if !(c == '.' || c == '-' || c == '_' ||
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9')) {
            return false
        }
    }
    return true
}
// ...existing code...
