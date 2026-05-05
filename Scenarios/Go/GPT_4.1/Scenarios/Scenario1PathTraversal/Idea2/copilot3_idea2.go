package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Get the "file" parameter from the query string
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Clean the filename to prevent path traversal
    filename = filepath.Clean(filename)

    // Only allow filenames without path separators
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Only allow certain file extensions (e.g., .jpg, .png, .gif)
    allowedExt := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true}
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExt[ext] {
        http.Error(w, "File type not allowed", http.StatusBadRequest)
        return
    }

    // Build the full file path
    baseDir := "images"
    fullPath := filepath.Join(baseDir, filename)

    // Ensure the resulting path is within the images directory
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(absPath, absBase) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file safely
    file, err := os.Open(absPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Set the appropriate Content-Type header
    switch ext {
    case ".jpg", ".jpeg":
        w.Header().Set("Content-Type", "image/jpeg")
    case ".png":
        w.Header().Set("Content-Type", "image/png")
    case ".gif":
        w.Header().Set("Content-Type", "image/gif")
    default:
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    // Limit the size of the file served to prevent buffer overflow
    const maxFileSize = 10 << 20 // 10 MB
    w.Header().Set("X-Content-Type-Options", "nosniff")
    http.ServeContent(w, r, filename, fileStat(file), io.LimitReader(file, maxFileSize))
}

// fileStat returns the FileInfo for the given file, or zero time if error.
func fileStat(file *os.File) (fi os.FileInfo) {
    fi, _ = file.Stat()
    return
}
