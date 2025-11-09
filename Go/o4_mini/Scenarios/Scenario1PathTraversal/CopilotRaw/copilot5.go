package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // base images directory (relative to working directory)
    baseDir := "images"

    // get filename from query parameter "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // clean the provided path
    cleanName := filepath.Clean(name)

    // reject absolute paths or attempts to traverse up
    if filepath.IsAbs(cleanName) || cleanName == ".." || strings.HasPrefix(cleanName, ".."+string(os.PathSeparator)) || strings.HasPrefix(cleanName, "..") {
        http.Error(w, "invalid file path", http.StatusForbidden)
        return
    }

    // resolve absolute paths for base and target and ensure target is inside base
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    target := filepath.Join(baseAbs, cleanName)
    targetAbs, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    rel, err := filepath.Rel(baseAbs, targetAbs)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "invalid file path", http.StatusForbidden)
        return
    }

    // serve the file
    http.ServeFile(w, r, targetAbs)
}
// ...existing code...
