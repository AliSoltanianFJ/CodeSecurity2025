package main

import (
    "log"
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

// ...existing code...
/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // get filename from query parameter "file"
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // base directory containing images (relative to working directory)
    baseDir := "images"

    // resolve absolute paths and prevent path traversal by ensuring
    // the requested file is inside the images directory
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Println("failed to resolve images directory:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // join then get absolute path of target
    target := filepath.Join(absBase, fname)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // ensure the target is inside the base directory
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
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
        log.Println("stat error:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // serve the file (http.ServeFile sets appropriate headers)
    http.ServeFile(w, r, absTarget)
}
