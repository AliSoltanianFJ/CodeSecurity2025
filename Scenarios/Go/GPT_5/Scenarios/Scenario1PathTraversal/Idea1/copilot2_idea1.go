package main

import (
    "io"
    "log"
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
    // get file name from query parameter "file"
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Normalize and take base name to avoid path traversal via ../ or absolute paths.
    // Also reject any path separators to keep filenames simple.
    clean := filepath.Base(filepath.Clean(fname))
    if clean == "." || clean == "" || strings.ContainsAny(clean, `/\`) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Base images directory (relative to working dir)
    imgDir := "images"

    // Resolve absolute paths and ensure the requested file is inside imgDir
    absBase, err := filepath.Abs(imgDir)
    if err != nil {
        log.Printf("failed to resolve images directory: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    target := filepath.Join(absBase, clean)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Open file and verify it's a regular file
    f, err := os.Open(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Printf("open error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    info, err := f.Stat()
    if err != nil {
        log.Printf("stat error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Read up to 512 bytes to detect content type and ensure it is an image
    head := make([]byte, 512)
    n, _ := f.Read(head)
    ctype := http.DetectContentType(head[:n])
    if !strings.HasPrefix(ctype, "image/") {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Seek back to start and serve the content with the detected content type
    if _, err := f.Seek(0, io.SeekStart); err != nil {
        log.Printf("seek error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    w.Header().Set("Content-Type", ctype)
    http.ServeContent(w, r, clean, info.ModTime(), f)
}
