package main

import (
    "io"
    "mime"
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
    // Validate method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize query parameter
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }
    // Limit length
    if len(name) > 255 {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }
    // Reject any path separators and allow only a safe character set
    if strings.ContainsAny(name, "/\\") {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }
    for _, rch := range name {
        if (rch >= 'a' && rch <= 'z') ||
            (rch >= 'A' && rch <= 'Z') ||
            (rch >= '0' && rch <= '9') ||
            rch == '.' || rch == '-' || rch == '_' {
            continue
        }
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Whitelist extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
    default:
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    const baseDir = "images"

    // Build absolute paths and ensure the final path is inside baseDir
    reqPath := filepath.Join(baseDir, name)
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absReq, err := filepath.Abs(reqPath)
    if err != nil {
        http.Error(w, "Invalid file", http.StatusBadRequest)
        return
    }
    // Ensure absReq is within absBase
    if absReq != absBase && !strings.HasPrefix(absReq, absBase+string(os.PathSeparator)) {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // Open file (streaming to avoid large memory usage)
    f, err := os.Open(absReq)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Cannot open file", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // Determine content type: prefer extension, fallback to sniffing first bytes
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        // Read first 512 bytes for sniffing
        buf := make([]byte, 512)
        n, _ := io.ReadFull(f, buf)
        contentType = http.DetectContentType(buf[:n])
        if _, err := f.Seek(0, io.SeekStart); err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
    }

    // Set safe headers (do not leak internal errors or paths)
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // Serve content with proper range support and efficient streaming
    if fi, err := f.Stat(); err == nil {
        http.ServeContent(w, r, fi.Name(), fi.ModTime(), f)
    } else {
        // Fallback: stream entire file
        if _, err := io.Copy(w, f); err != nil {
            // If client disconnects or copy fails, do not expose internal info
            return
        }
    }
}
