// ...existing code...
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

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    name := r.URL.Query().Get("file")
    if name == "" || len(name) > 255 {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    if filepath.Base(name) != name {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    if !regexp.MustCompile(`^[A-Za-z0-9._-]+$`).MatchString(name) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg":
    default:
        http.Error(w, "unsupported file", http.StatusBadRequest)
        return
    }

    base := "images"
    absBase, err := filepath.Abs(base)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    target := filepath.Join(absBase, name)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    if !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) && absTarget != absBase {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    info, err := os.Stat(absTarget)
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    http.ServeFile(w, r, absTarget)
}
// ...existing code...
