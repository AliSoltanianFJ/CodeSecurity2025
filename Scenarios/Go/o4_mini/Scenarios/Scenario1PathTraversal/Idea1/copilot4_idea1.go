// ...existing code...
package main

import (
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
    // allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // read filename from query
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

    // clean user input and reject attempts to traverse up
    name = filepath.Clean(name)
    if name == ".." || strings.HasPrefix(name, ".."+string(os.PathSeparator)) || strings.HasPrefix(name, "../") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // allow only common image extensions
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]struct{}{
        ".png":  {},
        ".jpg":  {},
        ".jpeg": {},
        ".gif":  {},
        ".webp": {},
    }
    if _, ok := allowed[ext]; !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    imagesDir := "images"
    baseAbs, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // join with the absolute base to avoid trickiness with relative joins
    target := filepath.Join(baseAbs, name)
    targetAbs, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Resolve symlinks for both base and target to prevent escaping via symlinks
    resolvedBase, err := filepath.EvalSymlinks(baseAbs)
    if err != nil {
        // if resolving base fails, fall back to baseAbs
        resolvedBase = baseAbs
    }
    resolvedTarget, err := filepath.EvalSymlinks(targetAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // ensure the resolved target is inside the resolved base directory
    rel, err := filepath.Rel(resolvedBase, resolvedTarget)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    // verify target exists and is a regular file
    info, err := os.Stat(resolvedTarget)
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
    http.ServeFile(w, r, resolvedTarget)
}
// ...existing code...
