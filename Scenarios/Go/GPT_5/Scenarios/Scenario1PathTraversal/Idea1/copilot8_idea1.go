// ...existing code...
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

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // get file name from query parameter "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // allow only simple filenames (no path separators). Adjust regex if you want to allow other chars.
    validFilename := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validFilename.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // allow only specific image extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // base images directory (relative to working dir)
    baseDir := "images"

    // resolve absolute paths and ensure requested file is inside baseDir
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

    // ensure the target is inside the base directory
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
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
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // set content-type based on extension and serve
    if ctype := mime.TypeByExtension(ext); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    http.ServeFile(w, r, absTarget)
}
// ...existing code...
