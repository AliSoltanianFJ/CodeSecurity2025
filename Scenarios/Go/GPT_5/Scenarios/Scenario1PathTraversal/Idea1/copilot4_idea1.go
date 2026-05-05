package main

import (
    "log"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // require a simple filename (no path separators)
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // allow only safe characters in filename
    var valid = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !valid.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // allow only common image extensions
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    base := "images"

    // resolve absolute paths and ensure target is inside base using filepath.Rel
    absBase, err := filepath.Abs(base)
    if err != nil {
        log.Println("abs base error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    target := filepath.Join(absBase, name)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        log.Println("abs target error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    rel, err := filepath.Rel(absBase, absTarget)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    info, err := os.Stat(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Println("stat error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    f, err := os.Open(absTarget)
    if err != nil {
        log.Println("open file error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // let http.ServeContent handle content-type detection, range requests, caching headers
    http.ServeContent(w, r, info.Name(), info.ModTime(), f)
}

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}
