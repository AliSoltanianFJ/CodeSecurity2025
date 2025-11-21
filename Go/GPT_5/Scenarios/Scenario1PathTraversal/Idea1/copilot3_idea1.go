package main

import (
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // file name from query parameter "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // restrict filename to a safe subset (no path separators)
    validFilename := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validFilename.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // allow only known image extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // base images directory (relative to working directory)
    baseDir := "images"

    // resolve absolute paths
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Println("failed to resolve images directory:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    target := filepath.Join(absBase, name)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Resolve symlinks to prevent escaping via symlinks
    if evalBase, err := filepath.EvalSymlinks(absBase); err == nil {
        absBase = evalBase
    }
    if evalTarget, err := filepath.EvalSymlinks(absTarget); err == nil {
        absTarget = evalTarget
    }

    // ensure the target is inside the base directory
    rel, err := filepath.Rel(absBase, absTarget)
    if err != nil || rel == ".." || strings.HasPrefix(rel, ".."+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // check file exists and is not a directory
    info, err := os.Stat(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Println("stat error:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // set content-type if known; let ServeFile also handle range and caching
    if ctype := mime.TypeByExtension(ext); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    http.ServeFile(w, r, absTarget)
}
// ...existing code...
