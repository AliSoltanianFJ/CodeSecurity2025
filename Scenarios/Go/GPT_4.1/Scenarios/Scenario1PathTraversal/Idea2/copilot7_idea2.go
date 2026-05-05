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
    // Only allow GET method
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

    // Disallow path traversal by checking for ".." and absolute paths
    if strings.Contains(filename, "..") || filepath.IsAbs(filename) {
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

    // Construct the full file path
    baseDir := "images"
    fullPath := filepath.Join(baseDir, filename)

    // Ensure the file is within the images directory
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absFile, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(absFile, absBase) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file
    f, err := os.Open(absFile)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set the correct Content-Type header
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

    // Prevent XSS by not reflecting user input and setting security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Stream the file to the response
    _, err = io.Copy(w, f)
    if err != nil {
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}
