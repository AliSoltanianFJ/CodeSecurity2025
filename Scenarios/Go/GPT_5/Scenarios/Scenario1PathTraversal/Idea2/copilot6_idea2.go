// ...existing code...
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

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Get and sanitize query parameter
    fileName := strings.TrimSpace(r.URL.Query().Get("file"))
    if fileName == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Limit length to a reasonable value
    if len(fileName) > 255 {
        http.Error(w, "file name too long", http.StatusBadRequest)
        return
    }

    // Disallow path separators to prevent path traversal
    if strings.ContainsAny(fileName, "/\\") {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only a safe set of filename characters
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]{1,255}$`)
    if !validName.MatchString(fileName) {
        http.Error(w, "invalid file name characters", http.StatusBadRequest)
        return
    }

    // Restrict allowed extensions to common safe image types (exclude SVG to avoid XSS vectors)
    ext := strings.ToLower(filepath.Ext(fileName))
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    if !allowedExts[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Resolve base images directory (relative to current working directory)
    baseDir := "images"
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        log.Println("error resolving images directory:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Construct full path and ensure it stays inside baseAbs
    fullPath := filepath.Join(baseAbs, fileName)
    fullAbs, err := filepath.Abs(fullPath)
    if err != nil {
        log.Println("error resolving file path:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the resolved path is under the base directory
    // Add path separator to avoid prefix collisions (e.g. /tmp/images and /tmp/images_evil)
    baseWithSep := baseAbs
    if !strings.HasSuffix(baseWithSep, string(os.PathSeparator)) {
        baseWithSep = baseWithSep + string(os.PathSeparator)
    }
    if fullAbs != baseAbs && !strings.HasPrefix(fullAbs, baseWithSep) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // Stat the file and ensure it's a regular file
    fi, err := os.Stat(fullAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Println("error stating file:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if !fi.Mode().IsRegular() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // Open file for serving
    f, err := os.Open(fullAbs)
    if err != nil {
        log.Println("error opening file:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Set safe headers
    // Provide explicit Content-Type based on extension; fallback to octet-stream
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    // Prevent MIME sniffing which can lead to XSS
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // Basic cache headers (optional, adjust as needed)
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // Serve the content using ServeContent which supports range requests and sets proper headers from the file
    http.ServeContent(w, r, fi.Name(), fi.ModTime().UTC(), f)
}

func main() {
    http.HandleFunc("/img", img)
    log.Println("Starting server on :8080")
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatal(err)
    }
}
// ...existing code...
