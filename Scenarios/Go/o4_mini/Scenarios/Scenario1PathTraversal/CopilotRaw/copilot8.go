// ...existing code...
package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // get file name from query param "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // use Base to strip any directory components and prevent traversal
    safeName := filepath.Base(name)
    if safeName == "." || safeName == "" {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    imgDir := "images"
    fp := filepath.Join(imgDir, safeName)

    // ensure final absolute path is inside the images directory
    if absDir, err := filepath.Abs(imgDir); err == nil {
        if absFp, err2 := filepath.Abs(fp); err2 == nil {
            if !(absFp == absDir || strings.HasPrefix(absFp, absDir+string(os.PathSeparator))) {
                http.Error(w, "invalid file path", http.StatusBadRequest)
                return
            }
        }
    }

    // check existence and that it's not a directory
    if fi, err := os.Stat(fp); err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    http.ServeFile(w, r, fp)
}
// ...existing code...
