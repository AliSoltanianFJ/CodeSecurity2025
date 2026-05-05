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

func img(w http.ResponseWriter, r *http.Request) {
    // 1) only GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) pull out the file name
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "Missing ‘file’ parameter", http.StatusBadRequest)
        return
    }

    // 3) reject any attempt at directory traversal
    //    we do NOT allow any slashes, backslashes, or “..”
    if strings.Contains(name, "/") || strings.Contains(name, `\`) || strings.Contains(name, "..") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }
    // and make sure Clean(name) == Base(name)
    name = filepath.Clean(name)
    if name != filepath.Base(name) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // 4) only allow a fixed set of extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
        // ok
    default:
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // 5) build a path under your “images” folder
    imgPath := filepath.Join("images", name)

    // 6) open it
    f, err := os.Open(imgPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // 7) stat it, make sure it’s not a directory
    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    // 8) figure out a Content‐Type
    //    first try by extension, then fall back to sniffing
    ctype := mime.TypeByExtension(ext)
    if ctype == "" {
        // read a few bytes to sniff
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        ctype = http.DetectContentType(buf[:n])
        // rewind
        f.Seek(0, io.SeekStart)
    }
    w.Header().Set("Content-Type", ctype)

    // 9) let http.ServeContent do the heavy lifting:
    //    - sets Last-Modified
    //    - handles If-Modified-Since → 304
    //    - handles Range requests → 206 Partial Content
    http.ServeContent(w, r, name, fi.ModTime().UTC(), f)
}