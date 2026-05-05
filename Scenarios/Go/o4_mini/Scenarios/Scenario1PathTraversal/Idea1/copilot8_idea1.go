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

    // reject absolute paths
    if filepath.IsAbs(name) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // clean and ensure there is no upward traversal component
    clean := filepath.Clean(name)
    parts := strings.Split(clean, string(os.PathSeparator))
    for _, p := range parts {
        if p == ".." {
            http.Error(w, "invalid file", http.StatusBadRequest)
            return
        }
    }
    // only allow common image extensions
    ext := strings.ToLower(filepath.Ext(clean))
    switch ext {
    case ".png", ".jpg", ".jpeg", ".gif", ".webp":
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    imagesDir := "images"
    target := filepath.Join(imagesDir, clean)

    // resolve symlinks and absolute paths for both base and target
    baseResolved, err := filepath.EvalSymlinks(imagesDir)
    if err != nil {
        // if imagesDir doesn't exist treat as server error
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    baseAbs, err := filepath.Abs(baseResolved)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    targetResolved, err := filepath.EvalSymlinks(target)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    targetAbs, err := filepath.Abs(targetResolved)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // ensure the target is inside the images directory
    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    // ensure target exists and is not a directory
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

    http.ServeFile(w, r, targetAbs)
}
// ...existing code...
