package main

import (
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
    // Expect GET parameter "file"
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Basic validation to prevent path traversal: disallow ".." and any path separators.
    if strings.Contains(filename, "..") || strings.ContainsAny(filename, "/\\") {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    imagesDir := "images"
    fullPath := filepath.Join(imagesDir, filename)

    // Ensure the target exists and is a file
    info, err := os.Stat(fullPath)
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Set a sensible Content-Type based on extension
    if ctype := mime.TypeByExtension(filepath.Ext(fullPath)); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    http.ServeFile(w, r, fullPath)
}
