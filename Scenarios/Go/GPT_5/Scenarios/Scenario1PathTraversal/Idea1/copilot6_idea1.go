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
    // read file name
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // restrict allowed filename characters (no path separators)
    validFilename := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validFilename.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // allowed image extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // base images directory
    baseDir := "images"
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        log.Println("failed to resolve images directory:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absBase = filepath.Clean(absBase)

    // build target path and perform initial containment check
    target := filepath.Join(absBase, name)
    target = filepath.Clean(target)
    if target != absBase && !strings.HasPrefix(target, absBase+string(filepath.Separator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // ensure file exists and is not a directory (follows symlinks)
    info, err := os.Stat(target)
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

    // resolve symlinks for both base and target and ensure resolved target remains inside base
    realBase, err := filepath.EvalSymlinks(absBase)
    if err != nil {
        // if the base cannot be resolved, fall back to absBase
        realBase = absBase
    }
    realTarget, err := filepath.EvalSymlinks(target)
    if err != nil {
        log.Println("failed to resolve target symlink:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    realBase, _ = filepath.Abs(realBase)
    realTarget, _ = filepath.Abs(realTarget)
    if realTarget != realBase && !strings.HasPrefix(realTarget, realBase+string(filepath.Separator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // set Content-Type and serve the resolved file
    if ctype := mime.TypeByExtension(ext); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    http.ServeFile(w, r, realTarget)
}
// ...existing code...
