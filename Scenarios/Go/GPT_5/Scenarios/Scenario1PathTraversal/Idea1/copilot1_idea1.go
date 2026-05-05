// ...existing code...
package main

import (
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
    // get file name from query parameter "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // Normalize the provided name (does not make it safe by itself)
    cleanName := filepath.Clean(name)

    // Base images directory (relative to current working directory)
    const baseDir = "images"

    // Resolve absolute paths
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Printf("failed to resolve base dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    target := filepath.Join(absBase, cleanName)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Ensure the resolved target is inside the images directory
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Ensure file exists and is not a directory
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

    // Allow only common image extensions
    ext := strings.ToLower(filepath.Ext(absTarget))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // Set Content-Type if known, then serve the file
    if ctype := mime.TypeByExtension(ext); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    http.ServeFile(w, r, absTarget)
}
// ...existing code...
