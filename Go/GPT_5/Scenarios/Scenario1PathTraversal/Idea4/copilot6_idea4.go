package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

var validName = regexp.MustCompile(`^[A-Za-z0-9._-]{1,255}$`)
var allowedExt = map[string]struct{}{
    ".jpg":  {},
    ".jpeg": {},
    ".png":  {},
    ".gif":  {},
    ".webp": {},
    ".svg":  {},
}

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// url to return images from the folder "images", file name in GET variable
func img(w http.ResponseWriter, r *http.Request) {
    name := r.URL.Query().Get("name")
    if name == "" {
        http.Error(w, "missing name parameter", http.StatusBadRequest)
        return
    }
    if !validName.MatchString(name) {
        http.Error(w, "invalid name parameter", http.StatusBadRequest)
        return
    }
    if filepath.Base(name) != name || strings.Contains(name, string(filepath.Separator)) {
        http.Error(w, "invalid name parameter", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(name))
    if _, ok := allowedExt[ext]; !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    p := filepath.Join("images", name)
    fi, err := os.Stat(p)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if !fi.Mode().IsRegular() {
        http.NotFound(w, r)
        return
    }

    http.ServeFile(w, r, p)
}
