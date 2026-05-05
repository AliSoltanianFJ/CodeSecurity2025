package main

import (
    "io"
    "mime"
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
    // Enforce safe HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Expect filename in ?file=
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic input sanity checks
    if strings.ContainsRune(name, '\x00') || len(name) > 255 {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Whitelist allowed image extensions (strict)
    ext := strings.ToLower(filepath.Ext(name))
    allowedExts := map[string]bool{
        ".jpg": true, ".jpeg": true, ".png": true, ".gif": true,
        ".webp": true, ".svg": true,
    }
    if !allowedExts[ext] {
        http.Error(w, "forbidden file type", http.StatusForbidden)
        return
    }

    // Base images directory (least privilege: serve only from here)
    baseDir := "images"
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Clean the requested name and join with base dir
    cleanName := filepath.Clean(name) // removes things like "a/../b"
    fullPath := filepath.Join(absBase, cleanName)

    absFull, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Ensure the resolved absolute path is inside the images directory (prevent traversal)
    if absFull != absBase && !strings.HasPrefix(absFull, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Stat the file, ensure it exists and is a regular file
    fi, err := os.Stat(absFull)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Enforce a conservative max file size to mitigate DoS (10 MB)
    const maxSize = 10 * 1024 * 1024
    if fi.Size() > maxSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Open file (os.File implements io.ReadSeeker required by ServeContent)
    f, err := os.Open(absFull)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Read up to 512 bytes to detect content type safely
    buf := make([]byte, 512)
    n, _ := f.Read(buf)
    contentType := http.DetectContentType(buf[:n])
    if contentType == "application/octet-stream" {
        // Fallback to extension-based type when detection is generic
        if t := mime.TypeByExtension(ext); t != "" {
            contentType = t
        }
    }

    // Reset reader to beginning for ServeContent
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("Content-Disposition", "inline; filename=\""+filepath.Base(absFull)+"\"")
    // Conservative caching (adjust per requirements)
    w.Header().Set("Cache-Control", "private, max-age=86400")

    // Serve the content using http helpers (supports range requests)
    http.ServeContent(w, r, filepath.Base(absFull), fi.ModTime(), f)
}