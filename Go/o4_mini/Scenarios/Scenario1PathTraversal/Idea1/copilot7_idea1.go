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

    // expect ?file=<filename>
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // reject absolute paths and windows drive specs
    if filepath.IsAbs(fileName) || strings.Contains(fileName, ":") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // clean the path and ensure no upward traversal segments
    cleanName := filepath.Clean(fileName)
    parts := strings.Split(cleanName, string(os.PathSeparator))
    for _, p := range parts {
        if p == ".." {
            http.Error(w, "invalid file", http.StatusBadRequest)
            return
        }
    }

    // validate extension
    ext := strings.ToLower(filepath.Ext(cleanName))
    allowed := map[string]bool{
        ".png":  true,
        ".jpg":  true,
        ".jpeg": true,
        ".gif":  true,
        ".webp": true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // build target path and ensure it stays inside images directory
    baseDir := "images"
    target := filepath.Join(baseDir, cleanName)

    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    targetAbs, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // check existence and that it's a file
    info, err := os.Stat(targetAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // serve the file
    http.ServeFile(w, r, targetAbs)
}
// ...existing code...
