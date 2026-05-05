// ...existing code...
package main

import (
    "net/http"
    "io"
    "mime"
    "regexp"
    "os"
    "path/filepath"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Check method
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and sanitize input
    name := strings.TrimSpace(r.URL.Query().Get("file"))
    if name == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }
    if len(name) > 255 {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Allow only safe filename characters to prevent traversal and injection
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Restrict to allowed image extensions
    allowedExt := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    ext := strings.ToLower(filepath.Ext(name))
    if !allowedExt[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Build safe path and ensure it stays inside the images directory
    imagesDir := "images"
    filename := filepath.Base(name) // strip any directory parts if present
    candidate := filepath.Join(imagesDir, filename)
    cleaned := filepath.Clean(candidate)

    // Verify the cleaned path is inside the images directory
    absImages, err := filepath.Abs(imagesDir)
    if err == nil {
        absCleaned, err2 := filepath.Abs(cleaned)
        if err2 == nil {
            if absCleaned != absImages && !strings.HasPrefix(absCleaned, absImages+string(os.PathSeparator)) {
                http.Error(w, "Invalid file parameter", http.StatusBadRequest)
                return
            }
        }
    }

    // Stat the file to ensure it exists and is not a directory
    info, err := os.Stat(cleaned)
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Open file (os.File implements io.ReadSeeker needed by ServeContent)
    f, err := os.Open(cleaned)
    if err != nil {
        http.NotFound(w, r)
        return
    }
    defer f.Close()

    // Determine content type: prefer extension, fallback to sniffing first 512 bytes
    ctype := mime.TypeByExtension(ext)
    if ctype == "" {
        var buf [512]byte
        n, _ := f.Read(buf[:])
        ctype = http.DetectContentType(buf[:n])
        // reset read pointer
        _, _ = f.Seek(0, io.SeekStart)
    }
    if ctype == "" {
        ctype = "application/octet-stream"
    }

    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Type", ctype)
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self';")

    // Serve with caching and range support
    modtime := info.ModTime()
    // Ensure modtime is valid for ServeContent
    if modtime.IsZero() {
        modtime = time.Now()
    }
    http.ServeContent(w, r, info.Name(), modtime, f)
}
// ...existing code...
