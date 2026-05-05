package main

import (
    "errors"
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// Security-relevant decisions:
// - Only serve files from the "images" directory, never allow path traversal (CWE-22).
// - Only allow files with specific, safe image extensions.
// - Use strict input validation and sanitization.
// - Do not leak internal errors to the client (CWE-209).
// - Log errors securely for auditing.
// - Use secure defaults (e.g., deny by default).
// - Do not hardcode secrets or credentials.

var (
    // Define the allowed image extensions (whitelist)
    allowedExtensions = map[string]struct{}{
        ".jpg":  {},
        ".jpeg": {},
        ".png":  {},
        ".gif":  {},
        ".webp": {},
        ".bmp":  {},
        ".svg":  {},
    }
    // Define the base directory for images (must be absolute)
    imageBaseDir = filepath.Join(".", "images")
)

func main() {
    // Strongly recommend running behind HTTPS in production (not shown here).
    http.HandleFunc("/img", img)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Principle of least privilege: only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the "file" parameter from the query string
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    // Sanitize filename: disallow path separators and null bytes
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") || strings.Contains(filename, "\x00") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Only allow files with safe extensions
    ext := strings.ToLower(filepath.Ext(filename))
    if _, ok := allowedExtensions[ext]; !ok {
        http.Error(w, "Unsupported file type", http.StatusUnsupportedMediaType)
        return
    }

    // Build the full file path securely
    imagePath := filepath.Join(imageBaseDir, filename)

    // Prevent path traversal by ensuring the resulting path is within the images directory
    absImagePath, err := filepath.Abs(imagePath)
    if err != nil || !strings.HasPrefix(absImagePath, filepath.Clean(imageBaseDir)+string(os.PathSeparator)) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file securely (read-only)
    file, err := os.Open(absImagePath)
    if err != nil {
        // Do not leak file system details
        if errors.Is(err, os.ErrNotExist) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Internal Server Error", http.StatusInternalServerError)
            log.Printf("Error opening file: %v", err) // Log for auditing
        }
        return
    }
    defer file.Close()

    // Set the correct Content-Type header
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)

    // Serve the file content
    http.ServeContent(w, r, filename, getFileModTime(file), file)
}

// getFileModTime returns the modification time of the file, or zero time on error
func getFileModTime(f *os.File) (modTime time.Time) {
    info, err := f.Stat()
    if err == nil {
        return info.ModTime()
    }
    return
}
