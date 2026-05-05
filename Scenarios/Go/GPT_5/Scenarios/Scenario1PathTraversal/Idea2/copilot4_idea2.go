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
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and validate input
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Accept only safe filename characters and limit length to avoid abuses
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]{1,255}$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Prevent directory separators and traversal
    if filepath.Base(name) != name {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Whitelist allowed extensions to reduce risk (no SVG)
    ext := strings.ToLower(filepath.Ext(name))
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

    // Build absolute paths and ensure the resolved path is inside imagesDir
    const imagesDir = "images"
    requestPath := filepath.Join(imagesDir, name)
    absRequestPath, err := filepath.Abs(requestPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // Ensure the file is within the images directory
    if absRequestPath != absImagesDir && !strings.HasPrefix(absRequestPath, absImagesDir+string(os.PathSeparator)) {
        http.Error(w, "invalid file location", http.StatusBadRequest)
        return
    }

    // Open file safely
    f, err := os.Open(absRequestPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    info, err := f.Stat()
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Determine content type from extension (avoid sniffing)
    ctype := mime.TypeByExtension(ext)
    if ctype == "" {
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // Restrictive CSP for image responses
    // Note: CSP is more useful on HTML responses, but add minimal headers to be safe.
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Serve content using ServeContent which handles range requests and sets caching headers
    http.ServeContent(w, r, info.Name(), info.ModTime().UTC(), f)

    // Optionally update access time (non-essential). Keep it bounded and safe.
    _ = os.Chtimes(absRequestPath, time.Now(), info.ModTime())
}
// ...existing code...
