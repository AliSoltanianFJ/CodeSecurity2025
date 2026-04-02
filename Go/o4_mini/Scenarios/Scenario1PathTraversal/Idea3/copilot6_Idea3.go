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

// img serves up a file from ./images whose name is passed
// as the "file" query-param.  Only a small whitelist of
// extensions is allowed, directory-traversal is blocked,
// and ServeContent is used for proper caching & range support.
func img(w http.ResponseWriter, r *http.Request) {
    // 1) Only GET & HEAD
    if r.Method != http.MethodGet && r.Method != http.MethodHead {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Grab the filename
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // 3) Reject anything with “..”
    if strings.Contains(name, "..") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // 4) Build a clean, absolute path under ./images
    const baseDir = "images"
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Clean the user-supplied filename (this collapses duplicate separators, etc.)
    cleanName := filepath.Clean(name)
    // Prevent an absolute path after Clean:
    cleanName = strings.TrimPrefix(cleanName, string(filepath.Separator))

    // Join it up
    fullPath := filepath.Join(absBase, cleanName)
    absFull, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }
    // Make sure the resulting path is still under our images directory
    if !strings.HasPrefix(absFull, absBase+string(filepath.Separator)) {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // 5) Open the file
    f, err := os.Open(absFull)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Server error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // 6) Stat it, reject directories
    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    // 7) Whitelist a few common image extensions
    ext := strings.ToLower(filepath.Ext(fi.Name()))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }
    if !allowed[ext] {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // 8) Try to determine Content-Type by extension,
    //    otherwise sniff the first 512 bytes.
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        contentType = http.DetectContentType(buf[:n])
        // rewind after sniff
        f.Seek(0, io.SeekStart)
    }
    w.Header().Set("Content-Type", contentType)

    // 9) Optional: set a long cache‐max‐age for static images
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // 10) Delegate to ServeContent (handles HEAD, Ranges, conditional GETs)
    http.ServeContent(w, r, fi.Name(), fi.ModTime(), f)
}