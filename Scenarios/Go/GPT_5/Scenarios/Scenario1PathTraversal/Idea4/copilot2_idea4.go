// ...existing code...
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

func img(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    name := r.URL.Query().Get("file")
    if name == "" || len(name) > 255 || !validName.MatchString(name) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    imagesDir := "images"
    absDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    target := filepath.Join(absDir, name)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    rel, err := filepath.Rel(absDir, absTarget)
    if err != nil || strings.HasPrefix(rel, "..") {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    fi, err := os.Stat(absTarget)
    if err != nil || fi.IsDir() {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    http.ServeFile(w, r, absTarget)
}