package main

import (
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize query parameter
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject any path separators immediately (prevents traversal attempts)
    if strings.ContainsAny(name, "/\\") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only safe filename characters
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only specific image extensions
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    if !allowed[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Base images directory (relative to executable). Keep it constant and controlled.
    const imagesDir = ".\\images" // Windows-aware literal; filepath.Abs will normalize
    const maxSize = 10 << 20      // 10 MB limit

    // Build absolute paths and ensure the resolved path is inside imagesDir
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    fullPath := filepath.Join(absBase, name)
    absFull, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // Ensure absFull is within absBase (prevents traversal)
    if absFull != absBase && !strings.HasPrefix(absFull, absBase+string(os.PathSeparator)) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Check file existence and size before streaming
    fi, err := os.Stat(absFull)
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }
    if fi.Size() > maxSize {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Open file for streaming (streams to response without loading whole file into memory)
    f, err := os.Open(absFull)
    if err != nil {
        http.NotFound(w, r)
        return
    }
    defer f.Close()

    // Set safe headers to help prevent XSS and content sniffing
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Use http.ServeContent to support ranges and efficient streaming from file
    http.ServeContent(w, r, fi.Name(), fi.ModTime(), f)
}
