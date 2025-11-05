package main

import (
    "errors"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// Security-relevant decisions:
// - Only files within the "images" directory are served (prevents path traversal, CWE-22).
// - Only allow specific image file extensions (prevents serving arbitrary files).
// - Input is sanitized and validated.
// - Errors are logged server-side but not exposed to clients (prevents info leakage, CWE-209).
// - Principle of least privilege: no file write or modification, only read.
// - No hardcoded secrets or credentials.
// - HTTPS is recommended for deployment (not enforced in code, but should be configured in production).

func main() {
    http.HandleFunc("/img", img)
    // In production, use HTTPS (ListenAndServeTLS) and secure configuration.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the 'file' parameter from the query string
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Sanitize and validate the file name
    cleanName, err := sanitizeFileName(fileName)
    if err != nil {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Only allow specific image extensions (whitelist)
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    ext := strings.ToLower(filepath.Ext(cleanName))
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusForbidden)
        return
    }

    // Build the absolute path to the images directory
    imagesDir := "images"
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Failed to resolve images directory: %v", err)
        return
    }

    // Join and clean the full file path
    fullPath := filepath.Join(absImagesDir, cleanName)
    absFullPath, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Failed to resolve file path: %v", err)
        return
    }

    // Ensure the file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absFullPath, absImagesDir+string(os.PathSeparator)) && absFullPath != absImagesDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file securely (read-only)
    f, err := os.Open(absFullPath)
    if err != nil {
        // Do not leak file existence info
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set appropriate Content-Type header
    contentType := mimeTypeByExt(ext)
    w.Header().Set("Content-Type", contentType)

    // Serve the file content
    http.ServeContent(w, r, cleanName, getFileModTime(f), f)
}

// sanitizeFileName ensures the file name is a base name and does not contain path separators or traversal
func sanitizeFileName(name string) (string, error) {
    if strings.Contains(name, string(os.PathSeparator)) || strings.Contains(name, "/") || strings.Contains(name, "\\") {
        return "", errors.New("invalid path separator in file name")
    }
    clean := filepath.Clean(name)
    if clean == "." || clean == ".." || strings.HasPrefix(clean, "..") {
        return "", errors.New("invalid file name")
    }
    return clean, nil
}

// mimeTypeByExt returns the MIME type for a given file extension
func mimeTypeByExt(ext string) string {
    switch ext {
    case ".jpg", ".jpeg":
        return "image/jpeg"
    case ".png":
        return "image/png"
    case ".gif":
        return "image/gif"
    case ".webp":
        return "image/webp"
    default:
        return "application/octet-stream"
    }
}

// getFileModTime returns the modification time of the file, or zero time if not available
func getFileModTime(f *os.File) (modTime time.Time) {
    info, err := f.Stat()
    if err == nil {
        return info.ModTime()
    }
    return
}
