package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

var (
    // allow only safe filename characters (no path separators)
    validFilename = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    // allowed image extensions (lowercase, including leading dot)
    allowedExts = map[string]struct{}{
        ".jpg":  {},
        ".jpeg": {},
        ".png":  {},
        ".gif":  {},
        ".webp": {},
    }
    // maximum allowed file size (10 MiB)
    maxImageSize int64 = 10 << 20
    // base directory holding images (relative to working directory)
    imageBaseDir = "images"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable "file" */
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get filename from query param "file"
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic filename validation: no path separators, whitelist characters
    if !validFilename.MatchString(filename) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    // Prevent filenames like ".hidden" or without extension
    ext := strings.ToLower(filepath.Ext(filename))
    if ext == "" {
        http.Error(w, "missing file extension", http.StatusBadRequest)
        return
    }
    if _, ok := allowedExts[ext]; !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Build absolute paths and ensure the file is inside the images directory
    baseAbs, err := filepath.Abs(imageBaseDir)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // Clean and join, then get absolute
    fullPath := filepath.Join(imageBaseDir, filename)
    fullAbs, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // Ensure the file is inside the base directory (prevent path traversal)
    rel, err := filepath.Rel(baseAbs, fullAbs)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // Open the file
    f, err := os.Open(fullAbs)
    if err != nil {
        // do not leak filesystem details
        http.NotFound(w, r)
        return
    }
    defer f.Close()

    // Stat to verify it's a regular file and check size
    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }
    if fi.Size() > maxImageSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Read up to 512 bytes to detect content type reliably
    head := make([]byte, 512)
    n, _ := f.Read(head)
    // Reset file read pointer
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Determine content type: prefer extension mapping, fall back to sniffing
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        contentType = http.DetectContentType(head[:n])
    }

    // Security headers
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "public, max-age=86400") // 1 day

    // Serve content with a size limit for safety
    limitedReader := io.LimitReader(f, maxImageSize)
    if _, err := io.Copy(w, limitedReader); err != nil {
        // If client disconnected or copy fails, do not expose internal errors
        return
    }
}
