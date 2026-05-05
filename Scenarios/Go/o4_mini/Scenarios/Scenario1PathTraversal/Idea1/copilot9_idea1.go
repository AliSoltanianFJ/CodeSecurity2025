package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...

func img(w http.ResponseWriter, r *http.Request) {
    // allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // get filename from query
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // reject absolute paths and any attempt to include path separators or null bytes
    if filepath.IsAbs(name) || strings.Contains(name, "\x00") || strings.Contains(name, "/") || strings.Contains(name, `\`) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // require that the provided name is a simple base name (no traversal)
    if filepath.Base(name) != name {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // validate extension
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]bool{".png": true, ".jpg": true, ".jpeg": true, ".gif": true, ".webp": true}
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    imagesDir := "images"
    target := filepath.Join(imagesDir, name)

    // resolve absolute paths and ensure target is inside imagesDir
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    // ensure the target exists and is a regular file
    info, err := os.Stat(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // serve the file
    http.ServeFile(w, r, absTarget)
}
// ...existing code...
