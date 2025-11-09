// ...existing code...
package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize input
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Must be a simple filename (no path separators)
    if filepath.Base(name) != name {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Limit length
    if len(name) > 255 {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Allow only safe filename characters
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) || strings.HasPrefix(name, ".") {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Restrict to safe image extensions (avoid SVG due to potential XSS)
    ext := strings.ToLower(filepath.Ext(name))
    allowedExt := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
    }
    if !allowedExt[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Resolve paths and ensure target is inside the images directory
    imagesDir := "images"
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    target := filepath.Join(absBase, name)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    rel, err := filepath.Rel(absBase, absTarget)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Ensure file exists and is a regular file
    info, err := os.Stat(absTarget)
    if err != nil || info.IsDir() {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")

    // Use http.ServeFile to stream the file safely (avoids loading entire file into memory)
    http.ServeFile(w, r, absTarget)
}
// ...existing code...
