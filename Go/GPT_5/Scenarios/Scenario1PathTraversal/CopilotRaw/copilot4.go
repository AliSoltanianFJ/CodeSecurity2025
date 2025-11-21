package main

import (
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}
// ...existing code...

func img(w http.ResponseWriter, r *http.Request) {
    // expect filename in GET param "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // sanitize: only use base name to prevent path traversal
    name = filepath.Base(name)
    if strings.Contains(name, "..") || strings.ContainsAny(name, `/\`) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // allow only common image extensions
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".svg":  true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusUnsupportedMediaType)
        return
    }

    imgPath := filepath.Join("images", name)

    f, err := os.Open(imgPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        log.Printf("error opening image %q: %v", imgPath, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // serve with correct content type and efficient ServeContent (uses file's modtime)
    ctype := mime.TypeByExtension(ext)
    if ctype == "" {
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)

    stat, err := f.Stat()
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    http.ServeContent(w, r, name, stat.ModTime(), f)
}
```// filepath:
