package main

import (
    "io"
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/img", img)

    server := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 20 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Println("starting server on :8080")
    if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get filename from query param "file"
    name := strings.TrimSpace(r.URL.Query().Get("file"))
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic sanity checks
    if len(name) > 255 {
        http.Error(w, "file name too long", http.StatusBadRequest)
        return
    }
    if strings.ContainsRune(name, 0) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }
    // Disallow path separators to prevent traversal attempts
    if strings.ContainsAny(name, `/\`) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only specific image extensions
    allowed := map[string]bool{
        ".png":  true,
        ".jpg":  true,
        ".jpeg": true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
    }
    ext := strings.ToLower(filepath.Ext(name))
    if !allowed[ext] {
        http.Error(w, "file type not allowed", http.StatusBadRequest)
        return
    }

    // Construct safe path and ensure it's inside the images directory
    imagesDir := "images"
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    candidate := filepath.Join(absImagesDir, filepath.Clean(name))

    absCandidate, err := filepath.Abs(candidate)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    // Ensure the resolved path is within the images directory
    if !strings.HasPrefix(absCandidate, absImagesDir+string(os.PathSeparator)) && absCandidate != absImagesDir {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Open the file for streaming
    f, err := os.Open(absCandidate)
    if err != nil {
        // don't leak filesystem details
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "unable to open file", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Read up to 512 bytes to detect content type (http.DetectContentType)
    buf := make([]byte, 512)
    n, _ := f.Read(buf) // ignore EOF error; n may be < 512

    // Determine content type safely: prefer extension lookup, fallback to sniffing
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        contentType = http.DetectContentType(buf[:n])
    }
    // Ensure not to serve as HTML/text which could enable XSS; default to octet-stream if unsure
    if strings.HasPrefix(contentType, "text/") {
        contentType = "application/octet-stream"
    }

    // Security headers
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "private, max-age=86400")
    // Restrictive CSP: only allow images from same origin when used in a page
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self';")

    // Write status and the first chunk already read, then stream the rest
    w.WriteHeader(http.StatusOK)
    if n > 0 {
        if _, err := w.Write(buf[:n]); err != nil {
            return
        }
    }
    // Copy the remainder of the file
    _, _ = io.Copy(w, f)
}
