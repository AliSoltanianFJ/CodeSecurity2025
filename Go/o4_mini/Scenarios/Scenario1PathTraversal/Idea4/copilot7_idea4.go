// ...existing code...
package main

import (
    "io"
    "net/http"
    "os"
    "path"
    "path/filepath"
    "strings"
    "time"
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

    // Get filename parameter
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic input constraints
    if len(name) > 255 {
        http.Error(w, "file name too long", http.StatusBadRequest)
        return
    }
    // Reject any path separators or traversal tokens — only allow a plain basename
    if strings.ContainsAny(name, "/\\") || strings.Contains(name, "..") {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }
    // normalize the base name and ensure it didn't change
    if path.Base(name) != name {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Whitelist allowed image extensions (lowercase, without dot)
    allowed := map[string]bool{
        "jpg":  true,
        "jpeg": true,
        "png":  true,
        "gif":  true,
        "webp": true,
        "svg":  true,
    }
    ext := strings.ToLower(strings.TrimPrefix(path.Ext(name), "."))
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Resolve images directory and requested file to absolute clean paths
    imagesDir := "images"
    imagesDirAbs, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    imagesDirAbs = filepath.Clean(imagesDirAbs)

    target := filepath.Join(imagesDirAbs, filepath.FromSlash(name))
    targetAbs, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    targetAbs = filepath.Clean(targetAbs)

    // Enforce that target is inside imagesDir (defends against traversal and symlink tricks)
    if targetAbs != imagesDirAbs && !strings.HasPrefix(targetAbs, imagesDirAbs+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Open file and perform checks
    f, err := os.Open(targetAbs)
    if err != nil {
        // don't reveal filesystem details
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    fi, err := f.Stat()
    if err != nil {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }
    // ensure it's a regular file
    if !fi.Mode().IsRegular() {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Read a small prefix to detect content type, then seek back
    buf := make([]byte, 512)
    n, _ := io.ReadFull(f, buf)
    if n > 512 {
        n = 512
    }
    // reset read pointer
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Detect content type and set secure response headers
    contentType := http.DetectContentType(buf[:n])
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")
    // conservative caching; adjust as appropriate for your application
    w.Header().Set("Cache-Control", "private, max-age=300")

    // Stream the file efficiently with correct modtime
    http.ServeContent(w, r, name, fi.ModTime().Truncate(time.Second), f)
}
// ...existing code...
