package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "mime"
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
    // Security controls:
    // - canonicalize input, resolve symlinks, enforce base directory containment
    // - whitelist extensions, set nosniff header, limit file size, avoid leaking paths

    const (
        baseDir     = "images"        // serve only from this directory (relative to process cwd)
        paramName   = "file"          // GET parameter containing file name
        maxFileSize = 10 << 20        // 10 MiB
    )

    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and basic sanitize filename
    name := r.URL.Query().Get(paramName)
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    if strings.ContainsRune(name, 0) { // reject null bytes
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Prevent absolute paths from client
    name = filepath.Clean(name)
    name = strings.TrimPrefix(name, string(filepath.Separator))
    // Disallow path elements that try to escape
    if strings.HasPrefix(name, ".."+string(filepath.Separator)) || name == ".." {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Resolve absolute paths
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Printf("internal error resolving base dir: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Join and then resolve symlinks to avoid symlink attacks
    candidate := filepath.Join(absBase, name)
    candidate, err = filepath.EvalSymlinks(candidate)
    if err != nil {
        // Don't reveal whether file exists
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    absCandidate, err := filepath.Abs(candidate)
    if err != nil {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    // Ensure candidate path is inside base directory
    // Add separator to avoid prefix collisions: "/base" vs "/base_other"
    prefix := absBase + string(os.PathSeparator)
    if !(absCandidate == absBase || strings.HasPrefix(absCandidate, prefix)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Whitelist allowed image extensions
    ext := strings.ToLower(filepath.Ext(absCandidate))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported media type", http.StatusUnsupportedMediaType)
        return
    }

    // Open file and check metadata
    f, err := os.Open(absCandidate)
    if err != nil {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    info, err := f.Stat()
    if err != nil || info.IsDir() {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    // Enforce maximum file size
    if info.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Set safe headers
    if ctype := mime.TypeByExtension(ext); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        // Fallback generic image type
        w.Header().Set("Content-Type", "application/octet-stream")
    }
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "public, max-age=86400") // adjust as needed

    // Serve content using the file's modtime to allow conditional requests
    http.ServeContent(w, r, info.Name(), info.ModTime().UTC(), f)
}
// ...existing code...
