package main

import (
    "log"
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

    // Get and sanitize input
    name := strings.TrimSpace(r.URL.Query().Get("file"))
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Strict filename whitelist: allow only letters, digits, dot, underscore, hyphen; limit length
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]{1,255}$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Define base directory (images)
    baseDir := filepath.Join(".", "images")
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        log.Printf("internal error resolving base directory: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Compose candidate path and ensure it is inside base directory (prevent path traversal & symlink escapes)
    candidate := filepath.Join(baseAbs, filepath.FromSlash(name))
    candidateAbs, err := filepath.Abs(candidate)
    if err != nil {
        log.Printf("internal error resolving file path: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure candidateAbs is within baseAbs
    sep := string(os.PathSeparator)
    if !strings.HasPrefix(candidateAbs, baseAbs+sep) {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // Stat file
    fi, err := os.Stat(candidateAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.Error(w, "file not found", http.StatusNotFound)
            return
        }
        log.Printf("stat error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // Open file
    f, err := os.Open(candidateAbs)
    if err != nil {
        log.Printf("open error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Determine content type from extension; do not rely on user input
    ctype := mime.TypeByExtension(strings.ToLower(filepath.Ext(name)))
    if ctype == "" {
        // Fallback: generic binary stream
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)
    // Prevent MIME sniffing
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // Optional: recommend no framing
    w.Header().Set("X-Frame-Options", "DENY")
    // Cache safely for a short duration
    w.Header().Set("Cache-Control", "public, max-age=300")

    // Use http.ServeContent to stream file and support range requests efficiently
    http.ServeContent(w, r, name, fi.ModTime().UTC().Truncate(time.Second), f)
}
// ...existing code...
