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
    const baseDir = "images"

    name := r.URL.Query().Get("file")
    if name == "" || len(name) > 255 {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    if strings.Contains(name, "..") || strings.ContainsAny(name, "/\\") {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    validName := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(name))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
    default:
        http.Error(w, "Unsupported media type", http.StatusUnsupportedMediaType)
        return
    }

    full := filepath.Join(baseDir, name)
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(full)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if absPath != absBase && !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    http.ServeFile(w, r, absPath)
}
