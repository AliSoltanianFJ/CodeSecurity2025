package main

import (
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // get file name from query parameter "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // basic sanity: reject embedded NULs
    if strings.IndexByte(name, 0) != -1 {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // only allow common image extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // base images directory (relative to working dir)
    baseDir := "images"
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Printf("failed to resolve images dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // join against the absolute base and clean the result
    target := filepath.Join(absBase, filepath.Clean(name))
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // ensure the requested file is inside the images directory
    rel, err := filepath.Rel(absBase, absTarget)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if rel == ".." || strings.HasPrefix(rel, ".."+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // check file exists and is not a directory
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
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // set content-type based on extension if available
    if c := mime.TypeByExtension(ext); c != "" {
        w.Header().Set("Content-Type", c)
    }

    // serve the file
    http.ServeFile(w, r, absTarget)
}
// ...existing code...
