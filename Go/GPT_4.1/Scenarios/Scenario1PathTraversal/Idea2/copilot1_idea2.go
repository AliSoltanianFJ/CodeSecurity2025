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

    // Get the "file" parameter
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Clean the file name to prevent directory traversal
    fileName = filepath.Clean(fileName)
    if strings.Contains(fileName, "..") || strings.HasPrefix(fileName, "/") || strings.HasPrefix(fileName, "\\") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Only allow certain file extensions (e.g., jpg, png, gif)
    allowedExt := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true}
    ext := strings.ToLower(filepath.Ext(fileName))
    if !allowedExt[ext] {
        http.Error(w, "File type not allowed", http.StatusBadRequest)
        return
    }

    // Build the full file path
    baseDir := "images"
    fullPath := filepath.Join(baseDir, fileName)

    // Ensure the file is within the images directory
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(absPath, absBase) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file
	f, err := os.Open(absPath)
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

    // Limit the size of the file served (e.g., 10MB max)
    const maxFileSize = 10 << 20 // 10 MB
    http.ServeContent(w, r, fileName, fStat(f), io.LimitReader(f, maxFileSize))
}

// Helper to get file info safely
func fStat(f *os.File) (fi os.FileInfo) {
    fi, _ = f.Stat()
    return
}
