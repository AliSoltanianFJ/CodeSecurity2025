package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strconv"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Configuration
    const maxFileSize = int64(10 * 1024 * 1024) // 10 MB
    baseDir := "images"                         // relative to working dir; ensure this dir is properly permissioned in deployment

    // Get filename param
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject obvious traversal attempts and ensure no path separators
    if strings.ContainsRune(name, '/') || strings.ContainsRune(name, '\\') {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    // Ensure filename is just the base name
    if filepath.Base(name) != name {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Whitelist extensions
    allowedExts := map[string]struct{}{
        ".png":  {},
        ".jpg":  {},
        ".jpeg": {},
        ".gif":  {},
        ".webp": {},
    }
    ext := strings.ToLower(filepath.Ext(name))
    if _, ok := allowedExts[ext]; !ok {
        http.Error(w, "file type not allowed", http.StatusBadRequest)
        return
    }

    // Build absolute paths and ensure target is inside baseDir
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    targetPath := filepath.Join(absBase, name)
    absTarget, err := filepath.Abs(targetPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    rel, err := filepath.Rel(absBase, absTarget)
    if err != nil || strings.HasPrefix(rel, "..") || rel == ".." {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // Open file
    f, err := os.Open(absTarget)
    if err != nil {
        // do not reveal whether file exists
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Stat and checks
    info, err := f.Stat()
    if err != nil || !info.Mode().IsRegular() {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    if info.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Read up to 512 bytes for content sniffing
    sniffBuf := make([]byte, 512)
    n, _ := io.ReadFull(f, sniffBuf)
    // Seek back to start for ServeContent
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    sniffBuf = sniffBuf[:n]

    // Determine MIME type: prefer by extension, then sniff
    detected := http.DetectContentType(sniffBuf)
    if detected == "application/octet-stream" {
        // fallback to extension-based type
        if t := mime.TypeByExtension(ext); t != "" {
            detected = t
        }
    }

    // Ensure MIME appears to be an image
    if !strings.HasPrefix(detected, "image/") {
        http.Error(w, "file content not allowed", http.StatusBadRequest)
        return
    }

    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // CSP: only allow self for images, deny everything else
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self';")
    // If served over TLS, advise HSTS
    if r.TLS != nil {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }

    // Set content headers and serve (supports range requests)
    w.Header().Set("Content-Type", detected)
    w.Header().Set("Content-Length", strconv.FormatInt(info.Size(), 10))
    http.ServeContent(w, r, name, info.ModTime().UTC(), f)

    // optional: log access (ensure logs do not contain sensitive info in production)
    _ = absTarget
    _ = time.Now()
}
