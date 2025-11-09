package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strconv"
    "strings"
    "log"
    "time"
)

// ...existing code...

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Enforce allowed method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Minimal security headers (add TLS/HSTS at TLS terminator)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self';")

    // Get filename from query param "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject obvious attempts at traversal or control characters
    if strings.ContainsAny(name, "/\\\x00") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Limit length to reasonable size
    if len(name) > 255 {
        http.Error(w, "File name too long", http.StatusBadRequest)
        return
    }

    // Strict whitelist for allowed filename characters
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Whitelist extensions
    ext := strings.ToLower(filepath.Ext(name))
    allowedExts := map[string]struct{}{
        ".jpg":  {},
        ".jpeg": {},
        ".png":  {},
        ".gif":  {},
        ".webp": {},
        ".svg":  {},
    }
    if _, ok := allowedExts[ext]; !ok {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Use configured images directory (relative to working dir)
    imagesDir := "images"

    // Resolve absolute paths and ensure the file is inside imagesDir (prevent traversal)
    absDir, err := filepath.Abs(imagesDir)
    if err != nil {
        log.Printf("img: failed to resolve images dir: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Join and clean path
    requestPath := filepath.Join(absDir, filepath.Clean(name))

    // Ensure requestPath is within absDir
    rel, err := filepath.Rel(absDir, requestPath)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Stat the file
    fi, err := os.Stat(requestPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.Error(w, "Not a file", http.StatusBadRequest)
        return
    }

    // Enforce max file size to mitigate large-file DoS
    const maxSize = int64(10 << 20) // 10 MiB
    if fi.Size() > maxSize {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Open file (os.File implements io.ReadSeeker required by ServeContent)
    f, err := os.Open(requestPath)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Let http.ServeContent handle range requests, caching headers, and efficient serving.
    // Set safe Cache-Control for clients/proxies; tune as appropriate for your environment.
    w.Header().Set("Cache-Control", "public, max-age=86400")
    w.Header().Set("Content-Length", strconv.FormatInt(fi.Size(), 10))

    // ServeContent will set Last-Modified and support conditional requests
    http.ServeContent(w, r, name, fi.ModTime().UTC(), f)
}

// ...existing code...
