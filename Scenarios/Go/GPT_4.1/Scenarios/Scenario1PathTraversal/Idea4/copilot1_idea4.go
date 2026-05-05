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
// - Only files within the "images" directory are served (prevents path traversal, CWE-22).
// - Only files with allowed extensions are served (prevents serving sensitive files).
// - All user input is sanitized and validated.
// - No sensitive error information is leaked to the client.
// - Use of io.Copy for efficient and safe file serving.
// - No hardcoded secrets or credentials.
// - HTTPS should be enforced in production (not shown here, but recommended).

func main() {
    http.HandleFunc("/img", img)
    // In production, use ListenAndServeTLS with valid certs to enforce HTTPS.
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the "file" parameter from the query string
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Sanitize filename: disallow path traversal and only allow specific extensions
    cleanName, err := sanitizeFilename(filename)
    if err != nil {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Construct the absolute path to the images directory
    const imagesDir = "images"
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Join and clean the full file path
    fullPath := filepath.Join(absImagesDir, cleanName)
    absFullPath, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absFullPath, absImagesDir+string(os.PathSeparator)) && absFullPath != absImagesDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file securely
    file, err := os.Open(absFullPath)
    if err != nil {
        // Do not leak file system details
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Detect and set the correct Content-Type header
    ctype := mime.TypeByExtension(filepath.Ext(cleanName))
    if ctype == "" {
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)

    // Set security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Serve the file content
    if _, err := io.Copy(w, file); err != nil {
        // Log error server-side if needed, but do not leak details to client
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}

// sanitizeFilename validates and sanitizes the filename parameter.
// Only allows filenames with safe characters and specific image extensions.
func sanitizeFilename(name string) (string, error) {
    // Disallow path separators and parent directory references
    if strings.Contains(name, "/") || strings.Contains(name, "\\") || strings.Contains(name, "..") {
        return "", errors.New("invalid filename")
    }

    // Only allow specific image extensions (add more as needed)
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".bmp":  true,
        ".webp": true,
        ".svg":  true,
    }

    ext := strings.ToLower(filepath.Ext(name))
    if !allowedExts[ext] {
        return "", errors.New("unsupported file extension")
    }

    // Optionally, enforce a filename pattern (e.g., alphanumeric, underscores, dashes)
    for _, c := range name {
        if !(c == '.' || c == '-' || c == '_' ||
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9')) {
            return "", errors.New("invalid character in filename")
        }
    }

    return name, nil
}
