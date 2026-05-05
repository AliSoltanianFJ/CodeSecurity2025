package main

import (
    "io"
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
    "time"
)

const (
    imageDir = "images"          // directory (relative to working dir) containing allowed images
    maxSize  = 10 * 1024 * 1024  // 10 MB maximum served file size
)

var (
    // strict filename whitelist: only simple filenames (no slashes), letters, numbers, dot, underscore, hyphen
    filenameRe = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)

    // allowed extensions mapped to MIME families we accept
    allowedExt = map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".svg":  true,
    }
)

func main() {
    http.HandleFunc("/img", img)
    // In production use ListenAndServeTLS with valid certificates and run the process with least privileges.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/*
 img serves files only from the configured imageDir. Security controls:
 - Enforce filename whitelist (no directories, no .., no absolute paths).
 - Canonicalize path and verify it is a descendant of imageDir using filepath.Rel.
 - Restrict by extension whitelist and confirm detected content type starts with image/.
 - Enforce a maximum file size to mitigate resource exhaustion.
 - Ensure returned headers prevent MIME sniffing and clickjacking, and avoid leaking filesystem details.
 - Use http.ServeContent to correctly handle range requests and conditional GETs.
*/
func img(w http.ResponseWriter, r *http.Request) {
    // Minimal information in error responses to avoid leaking filesystem details.
    errRespond := func(code int, msg string) {
        http.Error(w, msg, code)
    }

    // 1) Get and validate parameter
    fileParam := r.URL.Query().Get("file")
    if fileParam == "" {
        errRespond(http.StatusBadRequest, "missing file parameter")
        return
    }

    // Reject any path separators or traversal tokens early
    if strings.Contains(fileParam, "/") || strings.Contains(fileParam, "\\") || strings.Contains(fileParam, "..") {
        errRespond(http.StatusBadRequest, "invalid file parameter")
        return
    }

    // Whitelist filename characters only
    if !filenameRe.MatchString(fileParam) {
        errRespond(http.StatusBadRequest, "invalid file name")
        return
    }

    // 2) Extension whitelist
    ext := strings.ToLower(filepath.Ext(fileParam))
    if !allowedExt[ext] {
        errRespond(http.StatusBadRequest, "unsupported file type")
        return
    }

    // 3) Resolve absolute paths and ensure requested path is inside imageDir
    absBase, err := filepath.Abs(imageDir)
    if err != nil {
        log.Printf("internal error resolving image dir: %v", err)
        errRespond(http.StatusInternalServerError, "internal server error")
        return
    }
    requested := filepath.Join(absBase, fileParam)
    absRequested, err := filepath.Abs(requested)
    if err != nil {
        log.Printf("internal error resolving requested file: %v", err)
        errRespond(http.StatusInternalServerError, "internal server error")
        return
    }
    rel, err := filepath.Rel(absBase, absRequested)
    if err != nil || strings.HasPrefix(rel, "..") {
        // Not a descendant of imageDir
        errRespond(http.StatusForbidden, "access denied")
        return
    }

    // 4) Stat and enforce regular file + size limit
    fi, err := os.Stat(absRequested)
    if err != nil {
        if os.IsNotExist(err) {
            errRespond(http.StatusNotFound, "file not found")
            return
        }
        log.Printf("stat error: %v", err)
        errRespond(http.StatusInternalServerError, "internal server error")
        return
    }
    if fi.IsDir() || !fi.Mode().IsRegular() {
        errRespond(http.StatusForbidden, "access denied")
        return
    }
    if fi.Size() > maxSize {
        errRespond(http.StatusRequestEntityTooLarge, "file too large")
        return
    }

    // 5) Open file and detect content type from initial bytes (defense-in-depth)
    f, err := os.Open(absRequested)
    if err != nil {
        log.Printf("open error: %v", err)
        errRespond(http.StatusInternalServerError, "internal server error")
        return
    }
    defer f.Close()

    // Read up to 512 bytes for content type detection
    head := make([]byte, 512)
    n, _ := f.Read(head)
    detected := http.DetectContentType(head[:n])
    if !strings.HasPrefix(detected, "image/") {
        // In some cases SVG may be detected as text/xml or application/xml; allow svg explicitly by ext
        if ext != ".svg" {
            errRespond(http.StatusUnsupportedMediaType, "file content is not an image")
            return
        }
    }
    // Reset read position for ServeContent
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        log.Printf("seek error: %v", err)
        errRespond(http.StatusInternalServerError, "internal server error")
        return
    }

    // 6) Set safe response headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // CSP is restrictive; allow only images from self if needed by clients embedding images
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")
    // Conservative caching: allow caching but keep it private
    w.Header().Set("Cache-Control", "private, max-age=3600")

    // Set Content-Type based on detected type; fallback to extension-to-mime
    if detected == "application/octet-stream" || detected == "" {
        if m := mime.TypeByExtension(ext); m != "" {
            detected = m
        }
    }
    w.Header().Set("Content-Type", detected)

    // 7) Serve the content efficiently and safely (handles ranges, modtime, conditional GET)
    http.ServeContent(w, r, fi.Name(), fi.ModTime().UTC(), f)

    // Note: ensure the process user has minimal filesystem privileges (imageDir read-only) and
    // run behind HTTPS in production (use ListenAndServeTLS). Monitor logs for repeated invalid attempts.
}
