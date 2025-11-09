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

    // reject absolute paths and Windows drive specifiers
    if filepath.IsAbs(name) || strings.Contains(name, ":") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // clean and normalize to slash-separated segments, then ensure no upward traversal segments
    clean := filepath.Clean(name)
    parts := strings.Split(filepath.ToSlash(clean), "/")
    for _, p := range parts {
        if p == ".." {
            http.Error(w, "invalid file", http.StatusBadRequest)
            return
        }
    }

    baseDir := "images"
    target := filepath.Join(baseDir, clean)

    // resolve absolute paths and ensure target is inside baseDir
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    targetAbs, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // ensure the target exists and is a file
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
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // serve the file
    http.ServeFile(w, r, targetAbs)
}
// ...existing code...
