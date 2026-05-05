package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize input
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    // Basic safety checks
    if strings.ContainsRune(name, 0) || len(name) > 255 {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Clean and ensure the value is a simple base name (no directories)
    clean := filepath.Clean(name)
    if clean != filepath.Base(clean) || strings.Contains(clean, "..") || strings.HasPrefix(clean, string(filepath.Separator)) {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Whitelist extensions to common safe image types
    ext := strings.ToLower(filepath.Ext(clean))
    allowedExts := map[string]struct{}{
        ".jpg":  {},
        ".jpeg": {},
        ".png":  {},
        ".gif":  {},
        ".webp": {},
    }
    if _, ok := allowedExts[ext]; !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Resolve absolute paths and ensure file is inside the images directory
    imagesDir := "images"
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    fullPath := filepath.Join(absImagesDir, clean)

    absFullPath, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if !strings.HasPrefix(absFullPath, absImagesDir+string(filepath.Separator)) && absFullPath != absImagesDir {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // Open and validate file
    f, err := os.Open(absFullPath)
    if err != nil {
        // Do not leak filesystem details
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    stat, err := f.Stat()
    if err != nil || !stat.Mode().IsRegular() {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // Read up to 512 bytes for content type detection, then rewind
    buf := make([]byte, 512)
    n, _ := f.Read(buf)
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        contentType = http.DetectContentType(buf[:n])
    }
    // Reset read position
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Set headers (do not expose internal paths or sensitive info)
    w.Header().Set("Content-Type", contentType)
    // Let browsers know it's an inline image
    w.Header().Set("Content-Disposition", "inline; filename=\""+filepath.Base(clean)+"\"")
    // Serve content with proper modtime for caching support
    http.ServeContent(w, r, filepath.Base(clean), stat.ModTime().Truncate(time.Second), f)
}
