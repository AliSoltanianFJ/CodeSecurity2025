package main

import (
    "errors"
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// Security Decisions:
// - Only serve files from the "images" directory (no subdirectories).
// - Only allow files with a strict whitelist of image extensions.
// - Sanitize and validate the filename parameter to prevent path traversal (CWE-22).
// - Do not leak filesystem or internal errors to the client.
// - Use secure defaults and avoid exposing sensitive information.

func main() {
    http.HandleFunc("/img", img)
    // In production, always use HTTPS (TLS) and never ListenAndServe on plain HTTP.
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    const imagesDir = "images"
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }

    // Get the "file" parameter from the query string
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject filenames with path separators or attempts at directory traversal
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") || strings.Contains(filename, "..") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Only allow files with whitelisted extensions
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Construct the absolute path securely
    imagePath := filepath.Join(imagesDir, filename)
    absImagePath, err := filepath.Abs(imagePath)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Ensure the file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absImagePath, absImagesDir+string(os.PathSeparator)) && absImagePath != absImagesDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file securely (read-only)
    f, err := os.Open(absImagePath)
    if err != nil {
        // Do not leak file existence or error details
        if errors.Is(err, os.ErrNotExist) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Internal error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // Set the appropriate Content-Type header
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    w.Header().Set("X-Content-Type-Options", "nosniff") // Prevent MIME sniffing

    // Serve the file content
    if _, err := io.Copy(w, f); err != nil {
        // Do not leak error details
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}
