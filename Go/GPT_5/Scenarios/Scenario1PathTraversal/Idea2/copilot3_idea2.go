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
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and validate input
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    if len(name) > 255 {
        http.Error(w, "file name too long", http.StatusBadRequest)
        return
    }

    // Disallow any path separators and ensure basename is unchanged
    if strings.ContainsAny(name, `/\`) || filepath.Base(name) != name {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only a safe set of characters (alphanum, dot, dash, underscore)
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Whitelist safe binary image extensions (avoid SVG/text-based types)
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Base images directory (relative to working directory). Do not reveal absolute paths in errors.
    baseDir := "images"

    // Build absolute paths and ensure the file resides inside baseDir (prevents path traversal)
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    fullPath := filepath.Join(baseAbs, name)
    // Clean and make absolute
    fullAbs, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    rel, err := filepath.Rel(baseAbs, fullAbs)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "invalid file location", http.StatusBadRequest)
        return
    }

    // Stat the file and validate it's a regular file and not too large
    fi, err := os.Stat(fullAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.Error(w, "file not found", http.StatusNotFound)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }
    // Limit served file size to 20 MiB to prevent abuse
    const maxSize = 20 << 20
    if fi.Size() > maxSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Open file for streaming
    f, err := os.Open(fullAbs)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Determine Content-Type safely:
    // Prefer mime.TypeByExtension, but fall back to detection on the first 512 bytes.
    ct := mime.TypeByExtension(ext)
    if ct == "" {
        // Read up to 512 bytes for detection then seek back
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        ct = http.DetectContentType(buf[:n])
        _, _ = f.Seek(0, io.SeekStart)
    }
    // Ensure we always set a content-type
    if ct == "" {
        ct = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ct)
    // Avoid exposing server internals; do not include absolute paths in headers or error messages.
    // Let clients cache for a short time
    w.Header().Set("Cache-Control", "public, max-age=3600")
    // Set Last-Modified so conditional requests work
    http.ServeContent(w, r, name, fi.ModTime().UTC().Truncate(time.Second), f)
}
