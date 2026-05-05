package main

import (
    "errors"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// Security Decision: Only allow serving files from the "images" directory, and only allow specific safe image extensions.
// Principle of Least Privilege: No file write or modification, only read access to a controlled directory.
// Input Validation: Strictly validate and sanitize the "file" parameter to prevent path traversal and other attacks.

func main() {
    // Secure default: Listen only on localhost for development; use HTTPS in production.
    http.HandleFunc("/img", img)
    log.Fatal(http.ListenAndServe("localhost:8080", nil))
}

// img serves images from the "images" directory, validating input to prevent path traversal and other attacks.
func img(w http.ResponseWriter, r *http.Request) {
    const imagesDir = "images"
    // Only allow these image extensions (case-insensitive)
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }

    // Get the "file" parameter from the query string
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Sanitize: Remove any path separators and only allow base file names
    cleanFileName := filepath.Base(fileName)
    if cleanFileName != fileName {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Check for allowed extensions
    ext := strings.ToLower(filepath.Ext(cleanFileName))
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Construct the absolute path to the image
    imagePath := filepath.Join(imagesDir, cleanFileName)
    absImagePath, err := filepath.Abs(imagePath)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Ensure the file is within the images directory (prevent path traversal)
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    if !strings.HasPrefix(absImagePath, absImagesDir+string(os.PathSeparator)) && absImagePath != absImagesDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file securely (read-only)
    f, err := os.Open(absImagePath)
    if err != nil {
        // Do not leak file system details
        if errors.Is(err, os.ErrNotExist) {
            http.Error(w, "File not found", http.StatusNotFound)
        } else {
            http.Error(w, "Internal error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // Set secure Content-Type header
    switch ext {
    case ".jpg", ".jpeg":
        w.Header().Set("Content-Type", "image/jpeg")
    case ".png":
        w.Header().Set("Content-Type", "image/png")
    case ".gif":
        w.Header().Set("Content-Type", "image/gif")
    case ".webp":
        w.Header().Set("Content-Type", "image/webp")
    default:
        // Should not reach here due to earlier check
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // Set security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Serve the file content
    _, err = io.Copy(w, f)
    if err != nil {
        // Do not leak internal errors
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}
