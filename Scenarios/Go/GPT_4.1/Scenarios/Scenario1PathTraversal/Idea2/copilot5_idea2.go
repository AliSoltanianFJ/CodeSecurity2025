package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the filename from the query parameter
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Clean the filename to prevent path traversal
    filename = filepath.Clean(filename)
    if strings.Contains(filename, "..") || strings.HasPrefix(filename, "/") || strings.HasPrefix(filename, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Only allow certain file extensions (e.g., jpg, png, gif)
    allowedExts := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true}
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExts[ext] {
        http.Error(w, "File type not allowed", http.StatusBadRequest)
        return
    }

    // Build the full file path
    baseDir := "images"
    fullPath := filepath.Join(baseDir, filename)

    // Ensure the file is within the images directory
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absFile, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(absFile, absBase) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file safely
    f, err := os.Open(absFile)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set the appropriate Content-Type header
    switch ext {
    case ".jpg", ".jpeg":
        w.Header().Set("Content-Type", "image/jpeg")
    case ".png":
        w.Header().Set("Content-Type", "image/png")
    case ".gif":
        w.Header().Set("Content-Type", "image/gif")
    default:
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    // Prevent XSS by not reflecting user input and not serving HTML
    w.Header().Set("X-Content-Type-Options", "nosniff")

    // Serve the file content
    _, err = io.Copy(w, f)
    if err != nil {
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}
