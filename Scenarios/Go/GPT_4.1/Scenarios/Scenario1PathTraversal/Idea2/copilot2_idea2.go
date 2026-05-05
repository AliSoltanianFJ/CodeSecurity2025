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

    // Only allow certain file extensions (e.g., .jpg, .png, .gif)
    allowedExts := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true}
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExts[ext] {
        http.Error(w, "File type not allowed", http.StatusForbidden)
        return
    }

    // Build the full path and ensure it's within the images directory
    baseDir := "images"
    fullPath := filepath.Join(baseDir, filename)
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(absPath, absBase) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file safely
    f, err := os.Open(absPath)
	if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set the correct content type
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

    // Limit the size of the file served (e.g., max 10MB)
    const maxFileSize = 10 << 20 // 10MB
    http.ServeContent(w, r, filename, fStatModTime(f), io.LimitReader(f, maxFileSize))
}

// Helper to get file mod time safely
func fStatModTime(f *os.File) (modTime time.Time) {
    fi, err := f.Stat()
    if err == nil {
        return fi.ModTime()
    }
    return
}
