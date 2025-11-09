// ...existing code...
package main

import (
    "net/http"
    "path"
    "strings"
)

// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // expect ?file=<filename>
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // reject any Windows-style separators in the query (do not treat '\' as a path separator here)
    if strings.Contains(name, "\\") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // clean as a URL-style path and disallow upward traversal
    clean := path.Clean("/" + name) // ensures a leading slash for consistent cleaning
    if strings.HasPrefix(clean, "/..") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    // drop leading slash because FileServer expects a relative path
    clean = strings.TrimPrefix(clean, "/")

    // validate extension
    ext := strings.ToLower(path.Ext(clean))
    switch ext {
    case ".png", ".jpg", ".jpeg", ".gif", ".webp":
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // serve from the images directory using FileServer rooted at that directory
    fs := http.FileServer(http.Dir("images"))
    r2 := r.Clone(r.Context())
    r2.URL.Path = "/" + clean
    fs.ServeHTTP(w, r2)
}
// ...existing code...
