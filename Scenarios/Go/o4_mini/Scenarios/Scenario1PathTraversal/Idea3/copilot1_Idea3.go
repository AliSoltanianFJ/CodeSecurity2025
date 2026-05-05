package main

import (
    "io"
    "mime"
    "net/http"
    "os"
    "path"
    "path/filepath"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// img serves a single image from the "images" directory.
// URL format:  /img?file=some-picture.jpg
func img(w http.ResponseWriter, r *http.Request) {
    // 1) fetch and clean the requested filename
    q := r.URL.Query().Get("file")
    if q == "" {
        http.Error(w, "file parameter is required", http.StatusBadRequest)
        return
    }

    // path.Clean will resolve things like "../../etc/passwd" => "../etc/passwd"
    // so we can easily detect attempts to escape.
    clean := path.Clean("/" + q)   // prepend slash so Clean is rooted
    clean = strings.TrimPrefix(clean, "/") // strip the leading slash

    // 2) restrict to a small whitelist of image extensions
    ext := strings.ToLower(filepath.Ext(clean))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg":
        // ok
    default:
        http.Error(w, "forbidden file type", http.StatusForbidden)
        return
    }

    // 3) build the full filesystem path
    const imageDir = "images"
    fullPath := filepath.Join(imageDir, clean)

    // 4) ensure that after filepath.Join+Clean we are still inside images/
    //    i.e. no “../” escapes
    rel, err := filepath.Rel(imageDir, fullPath)
    if err != nil || strings.HasPrefix(rel, ".."+string(os.PathSeparator)) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // 5) open the file
    f, err := os.Open(fullPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "internal server error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // 6) stat it so we can pass ModTime to ServeContent
    fi, err := f.Stat()
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if fi.IsDir() {
        // should never happen if your fs layout is sane, but just in case:
        http.NotFound(w, r)
        return
    }

    // 7) determine the MIME type
    contentType := mime.TypeByExtension(ext)
    if contentType == "" {
        // fallback to sniffing the first 512 bytes
        buf := make([]byte, 512)
        n, _ := io.ReadFull(f, buf)
        contentType = http.DetectContentType(buf[:n])
        // rewind
        if _, err := f.Seek(0, io.SeekStart); err != nil {
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }
    }
    w.Header().Set("Content-Type", contentType)

    // 8) serve it with proper support for Range requests, conditional GETs, etc.
    http.ServeContent(w, r, fi.Name(), fi.ModTime(), f)
}