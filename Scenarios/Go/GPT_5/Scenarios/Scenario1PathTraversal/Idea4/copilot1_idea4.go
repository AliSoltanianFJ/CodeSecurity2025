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
    const imagesDir = "./images"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    if len(name) > 255 {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }
    allowedName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !allowedName.MatchString(name) {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg":
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    base := filepath.Base(name)
    if base != name {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    absDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    target := filepath.Join(absDir, name)
    cleanTarget := filepath.Clean(target)
    if !strings.HasPrefix(cleanTarget, absDir+string(os.PathSeparator)) && cleanTarget != absDir {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    fi, err := os.Stat(cleanTarget)
    if err != nil || fi.IsDir() {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    http.ServeFile(w, r, cleanTarget)
}
