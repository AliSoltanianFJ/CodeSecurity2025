package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strconv"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // ...existing code...

    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize query parameter (expected "file")
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic length check
    if len(name) > 255 {
        http.Error(w, "file name too long", http.StatusBadRequest)
        return
    }

    // Allow only a safe set of characters in filenames (no path separators)
    // Permit letters, numbers, dot, underscore, hyphen
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) || strings.ContainsAny(name, `/\`) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Only allow a whitelist of extensions to reduce risk (exclude SVG to avoid inline script XSS)
    ext := strings.ToLower(filepath.Ext(name))
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
    }
    if !allowedExts[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Build absolute paths and prevent path traversal by ensuring the resolved file path
    // is inside the images directory.
    baseDir := filepath.Join(".", "images")
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // Use Join with the validated filename
    reqPath := filepath.Join(absBase, name)
    absReq, err := filepath.Abs(reqPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // Ensure the requested path is within the base directory
    if absReq != absBase && !strings.HasPrefix(absReq, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Open file safely
    f, err := os.Open(absReq)
    if err != nil {
        // Don't leak filesystem details
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Ensure it is a regular file
    info, err := f.Stat()
    if err != nil || info.IsDir() {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // Enforce a maximum file size (e.g., 10 MiB) to avoid resource exhaustion
    const maxFileSize = 10 * 1024 * 1024 // 10 MiB
    if info.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Determine content type by extension first, fallback to sniffing
    contentType := mime.TypeByExtension(ext)
    var buf [512]byte
    if contentType == "" {
        n, _ := io.ReadFull(f, buf[:])
        contentType = http.DetectContentType(buf[:n])
        // Reset read offset to beginning
        _, err = f.Seek(0, io.SeekStart)
        if err != nil {
            http.Error(w, "server error", http.StatusInternalServerError)
            return
        }
    }

    // Set safe headers; avoid exposing server filesystem paths
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("Content-Length", strconv.FormatInt(info.Size(), 10))
    // Prefer inline display; don't include original path
    w.Header().Set("Content-Disposition", "inline; filename=\""+name+"\"")
    // Basic caching headers (optional)
    w.Header().Set("Cache-Control", "public, max-age=3600")
    w.Header().Set("Expires", time.Now().Add(time.Hour).UTC().Format(http.TimeFormat))

    // Stream file to client
    // io.Copy is safe in Go (no buffer overflow); it will handle efficient transfer
    _, err = io.Copy(w, f)
    if err != nil {
        // Connection might be closed by client; do not reveal internal errors
        return
    }

    // ...existing code...
}
