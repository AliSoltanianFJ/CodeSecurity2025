package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // ...existing code...

    // Allow only GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and validate parameter
    fileParam := r.URL.Query().Get("file")
    if fileParam == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject path separators; only allow a base filename (no directories)
    if filepath.Base(fileParam) != fileParam {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Enforce reasonable length
    if len(fileParam) > 255 {
        http.Error(w, "file parameter too long", http.StatusBadRequest)
        return
    }

    // Whitelist allowed extensions (lowercase)
    ext := strings.ToLower(filepath.Ext(fileParam))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".svg":  true,
    }
    if !allowed[ext] {
        http.Error(w, "forbidden file type", http.StatusForbidden)
        return
    }

    // Base directory for images (keep relative to binary or configured path)
    const imagesDir = "images"

    // Resolve absolute paths and ensure the requested file is contained in imagesDir
    joined := filepath.Join(imagesDir, fileParam)
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(joined)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    // Ensure absPath is within absBase
    if absPath != absBase && !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        http.Error(w, "invalid file location", http.StatusBadRequest)
        return
    }

    // Open the file and perform safe checks
    f, err := os.Open(absPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Prevent serving directories
    fi, err := f.Stat()
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    // Enforce a maximum allowed file size to mitigate DoS (example: 10 MiB)
    const maxSize int64 = 10 << 20
    if fi.Size() > maxSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Security response headers (minimal, application / proxy should set CSP, HSTS, etc.)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // Serve content safely (http.ServeContent handles range requests and sets proper headers).
    // Reset read position to start (ServeContent will read from current offset).
    _, _ = f.Seek(0, io.SeekStart)
    http.ServeContent(w, r, fileParam, fi.ModTime().UTC(), f)

    // ...existing code...
}
