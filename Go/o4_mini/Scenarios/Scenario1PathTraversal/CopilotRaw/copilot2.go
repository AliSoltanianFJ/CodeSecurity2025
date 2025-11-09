package main

import (
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

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Get filename from query
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject absolute paths and windows drive specifications
    if filepath.IsAbs(name) || strings.Contains(name, ":") {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    // Clean the provided name and prevent upward traversal
    cleanName := filepath.Clean(name)
    if cleanName == ".." || strings.HasPrefix(cleanName, ".."+string(os.PathSeparator)) || strings.HasPrefix(cleanName, "..") {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    baseDir := "images"
    joined := filepath.Join(baseDir, cleanName)

    // Resolve absolute paths and ensure the requested file is inside baseDir
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    targetAbs, err := filepath.Abs(joined)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // Ensure target is within base (either same or a child)
    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    // Check existence and that it's a file
    info, err := os.Stat(targetAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusForbidden)
        return
    }

    // Serve the file
    http.ServeFile(w, r, targetAbs)
}

// ...existing code...
