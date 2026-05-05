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
// - Only allow image files with specific extensions (whitelist).
// - Prevent path traversal by cleaning and validating the requested filename.
// - Do not leak filesystem structure or errors to the client.
// - Use least privilege: only serve files from the "images" directory.
// - Set secure HTTP headers to prevent caching and XSS.
// - Do not hardcode secrets or credentials.

func main() {
    http.HandleFunc("/img", img)
    // In production, always use HTTPS (TLS) for ListenAndServe.
    // http.ListenAndServeTLS(":8080", "cert.pem", "key.pem", nil)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Set secure headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "no-store")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Only allow GET method
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the 'file' parameter from the query string
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    // Allow only specific image extensions (whitelist)
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusUnsupportedMediaType)
        return
    }

    // Clean the filename to prevent path traversal
    cleanName := filepath.Clean(filename)
    if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Build the absolute path to the image directory
    imageDir := "images"
    absImageDir, err := filepath.Abs(imageDir)
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Build the absolute path to the requested file
    absFilePath, err := filepath.Abs(filepath.Join(imageDir, cleanName))
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absFilePath, absImageDir+string(os.PathSeparator)) && absFilePath != absImageDir {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // Open the file securely
    file, err := os.Open(absFilePath)
    if err != nil {
        // Do not leak file existence or errors
        http.Error(w, "Not Found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Detect the content type
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        // Fallback to detecting from content
        buf := make([]byte, 512)
        n, _ := file.Read(buf)
        contentType = http.DetectContentType(buf[:n])
        _, _ = file.Seek(0, io.SeekStart) // Reset file pointer
    }
    w.Header().Set("Content-Type", contentType)

    // Serve the file content
    if _, err := io.Copy(w, file); err != nil {
        // Do not leak internal errors
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
}
