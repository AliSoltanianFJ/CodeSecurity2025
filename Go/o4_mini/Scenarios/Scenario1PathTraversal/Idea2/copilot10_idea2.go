// ...existing code...
package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "unicode"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    const baseDir = "images"
    const maxFilenameLen = 255
    const maxFileSize = 10 << 20 // 10 MB

    // Validate input parameter
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    if len(fname) > maxFilenameLen {
        http.Error(w, "file name too long", http.StatusBadRequest)
        return
    }

    // Strict allowlist for filename characters (prevents traversal and surprises)
    for _, ch := range fname {
        if !(unicode.IsLetter(ch) || unicode.IsDigit(ch) || ch == '-' || ch == '_' || ch == '.') {
            http.Error(w, "invalid characters in file name", http.StatusBadRequest)
            return
        }
    }

    // Disallow path separators and parent refs explicitly
    if strings.ContainsAny(fname, `/\`) || strings.Contains(fname, "..") {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only safe image extensions (SVG excluded to avoid possible script vectors)
    ext := strings.ToLower(filepath.Ext(fname))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Build and resolve absolute paths and ensure file stays under baseDir
    requested := filepath.Join(baseDir, fname)
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    reqAbs, err := filepath.Abs(requested)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // Ensure reqAbs is inside baseAbs
    if reqAbs != baseAbs && !strings.HasPrefix(reqAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // Open file and check metadata
    f, err := os.Open(reqAbs)
    if err != nil {
        // don't reveal filesystem details
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    if fi.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Security headers to reduce XSS / sniffing risk
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

    // Determine content type: prefer extension mapping, fallback to sniffing
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        // Read small prefix to detect
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        mimeType = http.DetectContentType(buf[:n])
        // reset read position
        if _, err := f.Seek(0, io.SeekStart); err != nil {
            http.Error(w, "server error", http.StatusInternalServerError)
            return
        }
    }
    w.Header().Set("Content-Type", mimeType)

    // Serve up to maxFileSize bytes (protection vs unexpected large files)
    lr := io.LimitReader(f, maxFileSize+1)
    if _, err := io.Copy(w, lr); err != nil {
        // client aborted or write error; don't leak details
        return
    }
}
// ...existing code...
