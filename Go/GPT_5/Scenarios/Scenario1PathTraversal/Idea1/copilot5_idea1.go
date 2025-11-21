// ...existing code...
package main

import (
    "io"
    "log"
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
    // read requested file name
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // reject absolute paths from user input
    if filepath.IsAbs(fname) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // reject embedded NUL bytes
    if strings.IndexByte(fname, 0) != -1 {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // base images directory
    baseDir := "images"
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Printf("failed to resolve images dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // join with cleaned user input and resolve absolute path
    candidate := filepath.Join(absBase, filepath.Clean(fname))
    absTarget, err := filepath.Abs(candidate)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // ensure the resolved path is inside the images directory
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // stat the file
    info, err := os.Stat(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Printf("stat error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // allow only common image extensions
    ext := strings.ToLower(filepath.Ext(absTarget))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // open file to sniff content-type and serve
    f, err := os.Open(absTarget)
    if err != nil {
        log.Printf("open error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // read up to 512 bytes for detection
    buf := make([]byte, 512)
    n, _ := f.Read(buf)

    // prefer mime.TypeByExtension but fall back to sniffing
    ctype := mime.TypeByExtension(ext)
    if ctype == "" {
        ctype = http.DetectContentType(buf[:n])
    }
    w.Header().Set("Content-Type", ctype)

    // reset read pointer and serve content with proper modtime
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        log.Printf("seek error: %v", err)
    }
    http.ServeContent(w, r, info.Name(), info.ModTime(), f)
}
// ...existing code...
