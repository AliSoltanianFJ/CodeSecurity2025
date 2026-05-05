// ...existing code...
package main

import (
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
    // get file name from query
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // Reject names that contain path separators or attempt traversal
    if name != filepath.Base(name) || strings.Contains(name, "..") {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Allow only common image extensions
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".bmp":  true,
        ".webp": true,
        ".svg":  true,
        ".ico":  true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Build safe path inside images directory
    fullPath := filepath.Join("images", name)

    // Ensure the file exists and is not a directory
    info, err := os.Stat(fullPath)
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Serve the file
    http.ServeFile(w, r, fullPath)
}
// ...existing code...
