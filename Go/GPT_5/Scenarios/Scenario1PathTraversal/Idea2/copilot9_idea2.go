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
    // ...existing code...
    const baseDir = "images"
    const maxFileSize = 10 << 20 // 10 MB

    // Validate input: only allow a simple filename (no slashes), letters, numbers, dot, dash, underscore
    file := r.URL.Query().Get("file")
    if file == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(file) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Whitelist allowed extensions (avoid SVG because of potential XSS via XML)
    ext := strings.ToLower(filepath.Ext(file))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Build and validate safe absolute path to prevent path traversal
    candidate := filepath.Join(baseDir, file)
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(candidate)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    rel, err := filepath.Rel(absBase, absPath)
    if err != nil || strings.HasPrefix(rel, "..") || strings.Contains(rel, ".."+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Open the file safely
    f, err := os.Open(absPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Prevent serving directories & enforce size limit
    info, err := f.Stat()
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }
    if info.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Set safe headers to reduce attack surface
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self';")

    // Serve file content (using a limited reader to be extra safe)
    rdr := io.LimitReader(f, maxFileSize+1)
    _, err = io.Copy(w, rdr)
    if err != nil {
        // If the client disconnected or write failed, do not expose internal details
        return
    }
    // ...existing code...
}
