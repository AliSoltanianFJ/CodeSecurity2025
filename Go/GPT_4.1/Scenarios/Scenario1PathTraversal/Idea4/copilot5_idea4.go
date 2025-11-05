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
// - Only serve files from the "images" directory, never allow path traversal (CWE-22).
// - Only allow specific image file extensions (whitelist).
// - Do not leak internal errors to the client (CWE-209).
// - Use secure defaults and strict input validation.

func main() {
    http.HandleFunc("/img", img)
    // In production, always use HTTPS (TLS) to protect data in transit.
    // http.ListenAndServeTLS(":8080", "cert.pem", "key.pem", nil)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Principle of least privilege: restrict to GET method only.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Strict input validation: only allow safe filenames.
    filename := r.URL.Query().Get("file")
    if !isValidImageFilename(filename) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Prevent path traversal: always join with a fixed directory and clean the path.
    const imagesDir = "images"
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        // Log internal error, do not leak details to client.
        log.Printf("Failed to resolve images directory: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Construct the absolute path to the requested file.
    absFilePath := filepath.Join(absImagesDir, filename)
    absFilePath, err = filepath.Abs(absFilePath)
    if err != nil {
        log.Printf("Failed to resolve file path: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Ensure the file is within the images directory (prevents path traversal).
    if !strings.HasPrefix(absFilePath, absImagesDir+string(os.PathSeparator)) && absFilePath != absImagesDir {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // Only serve files that exist and are regular files.
    info, err := os.Stat(absFilePath)
    if err != nil || info.IsDir() {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    // Set secure headers.
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "private, max-age=86400")

    // Serve the file with correct content type.
    http.ServeFile(w, r, absFilePath)
}

// isValidImageFilename validates the filename to prevent path traversal and only allow certain extensions.
func isValidImageFilename(name string) bool {
    if name == "" || strings.Contains(name, string(os.PathSeparator)) || strings.Contains(name, "..") {
        return false
    }
    // Only allow specific image extensions (whitelist).
    allowedExts := map[string]struct{}{
        ".jpg":  {},
        ".jpeg": {},
        ".png":  {},
        ".gif":  {},
        ".webp": {},
        ".bmp":  {},
        ".svg":  {},
    }
    ext := strings.ToLower(filepath.Ext(name))
    _, ok := allowedExts[ext]
    return ok
}
