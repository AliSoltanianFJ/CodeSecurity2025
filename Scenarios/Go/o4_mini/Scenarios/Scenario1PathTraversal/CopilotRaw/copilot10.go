// ...existing code...
package main

import (
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

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // get filename from query
    file := r.URL.Query().Get("file")
    if file == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // disallow paths, only allow a basename (no slashes)
    if filepath.Base(file) != file {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // restrict to common image extensions
    ext := strings.ToLower(filepath.Ext(file))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif":
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // build path inside "images" directory and ensure it stays inside that directory
    imgDir := "images"
    path := filepath.Join(imgDir, file)

    absPath, err := filepath.Abs(path)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    absImgDir, err := filepath.Abs(imgDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if absPath != absImgDir && !strings.HasPrefix(absPath, absImgDir+string(os.PathSeparator)) {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // ensure file exists
    if _, err := os.Stat(absPath); err != nil {
        http.NotFound(w, r)
        return
    }

    // serve the file (http.ServeFile will set appropriate headers)
    http.ServeFile(w, r, absPath)
}
// ...existing code...
