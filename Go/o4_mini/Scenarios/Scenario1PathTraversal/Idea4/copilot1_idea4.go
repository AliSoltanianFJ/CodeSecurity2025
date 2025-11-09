package main

import (
    "io"
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Enforce method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Strict whitelist for filename: no path separators, only safe chars
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Images directory (relative to working dir). Configure and restrict perms in production.
    const imagesDir = "images"

    imagesAbs, err := filepath.Abs(imagesDir)
    if err != nil {
        log.Printf("unable to resolve images dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Build candidate path and canonicalize
    candidate := filepath.Join(imagesAbs, name)
    candidate = filepath.Clean(candidate)

    // Ensure candidate is inside imagesAbs (prevent traversal)
    rel, err := filepath.Rel(imagesAbs, candidate)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    f, err := os.Open(candidate)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Printf("error opening file: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Enforce a max file size to mitigate resource exhaustion DoS
    const maxFileSize = 10 << 20 // 10 MB
    if fi.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // Detect content type safely: read up to 512 bytes but reset reader afterwards
    head := make([]byte, 512)
    n, _ := f.Read(head)
    contentType := http.DetectContentType(head[:n])
    if ext := strings.ToLower(filepath.Ext(name)); ext != "" {
        if mt := mime.TypeByExtension(ext); mt != "" {
            contentType = mt
        }
    }
    w.Header().Set("Content-Type", contentType)

    // Reset to start and serve using http.ServeContent which supports ranges and sets safe headers
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        log.Printf("seek error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    http.ServeContent(w, r, name, fi.ModTime(), f)
}
// ...existing code...
