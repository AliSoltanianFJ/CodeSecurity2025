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
    const imagesDir = "images"

    file := r.URL.Query().Get("file")
    if file == "" || len(file) > 255 {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    if !regexp.MustCompile(`^[A-Za-z0-9._-]+$`).MatchString(file) {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    imagesAbs, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    filePath := filepath.Clean(filepath.Join(imagesAbs, file))
    fileAbs, err := filepath.Abs(filePath)
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    if fileAbs != imagesAbs && !strings.HasPrefix(fileAbs, imagesAbs+string(os.PathSeparator)) {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    fi, err := os.Stat(fileAbs)
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    http.ServeFile(w, r, fileAbs)
}
