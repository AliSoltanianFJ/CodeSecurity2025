// ...existing code...
package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strconv"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    const imagesDir = "./images"
    const maxFileSize = 10 << 20 // 10 MB

    // Only accept GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and validate filename param
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    if len(name) > 255 {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }
    if name == "." || name == ".." || name != filepath.Base(name) || strings.ContainsAny(name, "/\\") {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Allow only safe characters to avoid injections
    var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Restrict extensions and map to content types
    ext := strings.ToLower(filepath.Ext(name))
    contentTypes := map[string]string{
        ".jpg":  "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png":  "image/png",
        ".gif":  "image/gif",
        ".webp": "image/webp",
        ".svg":  "image/svg+xml",
    }
    ct, ok := contentTypes[ext]
    if !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Resolve absolute paths and ensure file is inside imagesDir
    imagesAbs, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    filePath := filepath.Join(imagesAbs, name)
    fileAbs, err := filepath.Abs(filePath)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // Ensure containment: file must start with imagesAbs + separator
    prefix := imagesAbs + string(os.PathSeparator)
    if !(fileAbs == imagesAbs || strings.HasPrefix(fileAbs, prefix)) {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // Open the file
    f, err := os.Open(fileAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // Stat and enforce size limits and not a directory
    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }
    if fi.Size() > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Secure headers
    w.Header().Set("Content-Type", ct)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "private, max-age=86400")
    w.Header().Set("Content-Length", strconv.FormatInt(fi.Size(), 10))

    // Stream file to response with an extra safety limit
    limited := io.LimitReader(f, maxFileSize+1)
    if _, err := io.Copy(w, limited); err != nil {
        // Connection error while streaming; do not expose internal details
        return
    }
}
// ...existing code...
