package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

var validName = regexp.MustCompile(`^[A-Za-z0-9._-]{1,255}$`)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    name := r.URL.Query().Get("file")
    if name == "" || !validName.MatchString(name) || strings.ContainsAny(name, `/\`) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    baseDir := "images"
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    target := filepath.Join(baseDir, name)
    targetAbs, err := filepath.Abs(filepath.Clean(target))
    if err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    info, err := os.Stat(targetAbs)
    if err != nil || info.IsDir() {
        http.NotFound(w, r)
        return
    }

    http.ServeFile(w, r, targetAbs)
}
