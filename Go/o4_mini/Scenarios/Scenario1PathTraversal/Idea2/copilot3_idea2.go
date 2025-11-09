// ...existing code...
package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate and sanitize input
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Limit filename length
    if len(fileName) > 255 {
        http.Error(w, "filename too long", http.StatusBadRequest)
        return
    }

    // Only allow safe filename characters (no slashes, no path traversal, no control chars)
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(fileName) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Prevent hidden files
    if strings.HasPrefix(fileName, ".") {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Resolve base and target paths and ensure target is inside images directory
    imagesDir := "images"
    joined := filepath.Join(imagesDir, fileName)

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

    // Ensure the resolved path is within the images directory
    if absPath != absBase && !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Stat the file and enforce size limit (protect against very large files)
    fi, err := os.Stat(absPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    const maxSize = 10 << 20 // 10 MB
    if fi.Size() > maxSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Determine MIME type from extension and set safe headers
    ext := strings.ToLower(filepath.Ext(fileName))
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        // default to a safe generic type for binary data
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "private, max-age=300")

    // Stream file content (no buffering of entire file) with basic error handling
    f, err := os.Open(absPath)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Copy content; file size already checked
    if _, err := io.Copy(w, f); err != nil {
        // Do not leak internal details
        http.Error(w, "failed to send file", http.StatusInternalServerError)
        return
    }
}
// ...existing code...
