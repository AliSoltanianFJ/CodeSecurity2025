package main

import (
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

func img(w http.ResponseWriter, r *http.Request) {
    name := r.URL.Query().Get("file")
    if name == "" || len(name) > 255 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }
    base := "images"
    reqPath := filepath.Join(base, name)
    absBase, err := filepath.Abs(base)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(reqPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if absPath != absBase && !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    f, err := os.Open(absPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()
    info, err := f.Stat()
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }
    http.ServeContent(w, r, info.Name(), info.ModTime(), f)
}
