package main

import (
    "net/http"
    "os"
    "path"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    baseDir := "images"

    raw := r.URL.Query().Get("name")
    if raw == "" {
        http.Error(w, "missing 'name' parameter", http.StatusBadRequest)
        return
    }

    // Disallow path traversal or any directory components: only a plain filename allowed
    if strings.ContainsAny(raw, "/\\") || path.Base(raw) != raw || strings.Contains(raw, "..") {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Resolve absolute paths and ensure the final path is inside baseDir
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    fullPath := filepath.Join(baseAbs, filepath.Clean(raw))

    // Extra safety: ensure resulting path is under baseAbs
    fullAbs, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(fullAbs, baseAbs+string(os.PathSeparator)) && fullAbs != baseAbs {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    info, err := os.Stat(fullAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    http.ServeFile(w, r, fullAbs)
    // ...existing code...
}
