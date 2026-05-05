package main

import (
    "net/http"
    "path/filepath"
    "os"
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
    base := "images"
    file := r.URL.Query().Get("file")
    if file == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Resolve absolute paths and prevent path traversal by ensuring the
    // requested file remains inside the images directory.
    baseAbs, err := filepath.Abs(base)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Clean the incoming file name and join with base directory.
    joined := filepath.Join(base, filepath.Clean("/"+file))
    pathAbs, err := filepath.Abs(joined)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    rel, err := filepath.Rel(baseAbs, pathAbs)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    info, err := os.Stat(pathAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    http.ServeFile(w, r, pathAbs)
}
