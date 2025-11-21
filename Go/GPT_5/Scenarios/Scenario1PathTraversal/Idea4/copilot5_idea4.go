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
    base := "images"
    baseAbs, err := filepath.Abs(base)
    if err != nil {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    filename := r.URL.Query().Get("file")
    if filename == "" || len(filename) > 255 {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") || strings.Contains(filename, "..") {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    ext := strings.ToLower(filepath.Ext(filename))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg":
    default:
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    target := filepath.Join(baseAbs, filename)
    targetAbs, err := filepath.Abs(filepath.Clean(target))
    if err != nil {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    if !(strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) || targetAbs == baseAbs) {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    info, err := os.Stat(targetAbs)
    if err != nil || info.IsDir() {
        http.Error(w, "Not Found", http.StatusNotFound)
        return
    }

    http.ServeFile(w, r, targetAbs)
}
