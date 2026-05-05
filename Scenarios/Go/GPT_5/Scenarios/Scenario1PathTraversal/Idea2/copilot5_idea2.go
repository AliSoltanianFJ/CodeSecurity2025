// ...existing code...
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

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and validate parameter
    fileParam := r.URL.Query().Get("file")
    if fileParam == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }
    if len(fileParam) > 255 {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Reject any path separators immediately
    if strings.Contains(fileParam, "/") || strings.Contains(fileParam, `\`) || strings.Contains(fileParam, string(os.PathSeparator)) {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Ensure it's a simple basename (no directories, no traversal)
    name := filepath.Base(fileParam)
    if name != fileParam {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Validate allowed characters in filename
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Only allow safe image extensions (no SVG to avoid embedded script risks)
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

    imagesDir := "images"

    // Build and verify the absolute path stays inside imagesDir
    targetPath := filepath.Join(imagesDir, name)
    absTarget, err := filepath.Abs(targetPath)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    // Ensure absTarget is inside absDir
    if absTarget != absDir && !strings.HasPrefix(absTarget, absDir+string(os.PathSeparator)) {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Stat and ensure it's a regular file
    fi, err := os.Stat(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Open the file
    f, err := os.Open(absTarget)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Determine content type (prefer extension, otherwise sniff)
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        contentType = http.DetectContentType(buf[:n])
        // Reset read pointer
        if _, err := f.Seek(0, io.SeekStart); err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
    }

    // Security headers
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "public, max-age=3600")

    // Serve content (supports ranges and sets Last-Modified)
    http.ServeContent(w, r, name, fi.ModTime(), f)
}
// ...existing code...
