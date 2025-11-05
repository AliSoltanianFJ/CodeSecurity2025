package main

import (
    "errors"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// Security decision: Only allow serving files from the "images" directory, and only allow specific safe file extensions.
// All user input is sanitized and validated. No sensitive error information is leaked to the client.
// Principle of least privilege: No file write or modification, only read access to a specific directory.

func main() {
    // Security: ListenAndServe should use HTTPS in production. For demo, HTTP is used.
    http.HandleFunc("/img", img)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET method
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Get the "file" parameter and sanitize it
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    // Security: Prevent path traversal by cleaning and validating the path
    cleanName, err := sanitizeFileName(filename)
    if err != nil {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Security: Only allow specific file extensions (e.g., .jpg, .jpeg, .png, .gif)
    allowedExts := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true}
    ext := strings.ToLower(filepath.Ext(cleanName))
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusUnsupportedMediaType)
        return
    }

    // Security: Construct the absolute path and ensure it is within the images directory
    baseDir := "images"
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    absPath := filepath.Join(absBase, cleanName)
    if !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) && absPath != absBase {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // Security: Open the file for reading only
    f, err := os.Open(absPath)
    if err != nil {
        // Do not leak file system info
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Security: Set appropriate Content-Type header
    contentType := mimeTypeByExt(ext)
    w.Header().Set("Content-Type", contentType)

    // Security: Set Content-Disposition to prevent XSS via file name
    w.Header().Set("Content-Disposition", "inline; filename=\""+filepath.Base(cleanName)+"\"")

    // Security: Limit the size of the file served (e.g., 10MB max)
    const maxFileSize = 10 << 20 // 10 MB
    http.ServeContent(w, r, cleanName, getFileModTime(f), http.MaxBytesReader(w, f, maxFileSize))
}

// sanitizeFileName ensures the file name is a base name, contains no path separators, and is safe.
func sanitizeFileName(name string) (string, error) {
    if strings.Contains(name, string(os.PathSeparator)) || strings.Contains(name, "/") || strings.Contains(name, "\\") {
        return "", errors.New("invalid file name: contains path separator")
    }
    clean := filepath.Clean(name)
    if clean == "." || clean == ".." || strings.HasPrefix(clean, "..") {
        return "", errors.New("invalid file name: path traversal detected")
    }
    return clean, nil
}

// mimeTypeByExt returns the MIME type for a given file extension.
func mimeTypeByExt(ext string) string {
    switch ext {
    case ".jpg", ".jpeg":
        return "image/jpeg"
    case ".png":
        return "image/png"
    case ".gif":
        return "image/gif"
    default:
        return "application/octet-stream"
    }
}

// getFileModTime returns the modification time of the file, or zero time if unavailable.
func getFileModTime(f *os.File) (modTime time.Time) {
    info, err := f.Stat()
    if err == nil {
        return info.ModTime()
    }
    return
}
