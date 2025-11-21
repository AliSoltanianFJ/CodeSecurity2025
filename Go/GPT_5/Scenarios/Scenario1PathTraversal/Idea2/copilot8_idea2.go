package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and sanitize filename from query parameter "file"
    fileName := strings.TrimSpace(r.URL.Query().Get("file"))
    if fileName == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic sanity checks: no path separators or traversal tokens
    if strings.Contains(fileName, "..") || strings.ContainsAny(fileName, `/\`) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Limit file name length to a reasonable amount
    if len(fileName) > 255 {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only specific image extensions
    ext := strings.ToLower(filepath.Ext(fileName))
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
    }
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Resolve absolute paths and ensure the target is inside the images directory
    baseDir := "images"
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Clean the filename and join with base directory
    cleanName := filepath.Clean(fileName)
    targetPath := filepath.Join(baseAbs, cleanName)

    // Resolve absolute path of target and ensure it's within baseAbs
    targetAbs, err := filepath.Abs(targetPath)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // Ensure targetAbs is either baseAbs or inside it
    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // Open the file and confirm it's a regular file
    f, err := os.Open(targetAbs)
    if err != nil {
        // don't leak internal error details
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    fi, err := f.Stat()
    if err != nil || !fi.Mode().IsRegular() {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // Restrict where content can be loaded from (images only)
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self';")

    // Serve content using ServeContent (uses file's modtime and supports range requests)
    http.ServeContent(w, r, fi.Name(), fi.ModTime(), f)
}
