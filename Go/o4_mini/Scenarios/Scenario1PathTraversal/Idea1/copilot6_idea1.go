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

    // expect ?file=<filename>
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // reject absolute paths and windows drive specs
    if filepath.IsAbs(name) || strings.Contains(name, ":") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // restrict to a simple filename (no directories) to avoid traversal via subdirs
    name = filepath.Base(name)

    // validate extension
    ext := strings.ToLower(filepath.Ext(name))
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

    imagesDir := "images"
    target := filepath.Join(imagesDir, name)

    // ensure the resolved target is inside imagesDir
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    rel, err := filepath.Rel(absBase, absTarget)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // check existence and that it's a file
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
