package main

import (
    "net/http"
    "path/filepath"
)

// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // get filename from "file" query parameter and strip any directory components
    fname := filepath.Base(r.URL.Query().Get("file"))
    if fname == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // reject obviously invalid names
    if fname == "." || fname == ".." {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    // serve only from the local "images" directory
    path := filepath.Join("images", fname)
    http.ServeFile(w, r, path)
}
// ...existing code...
