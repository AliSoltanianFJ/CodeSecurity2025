// ...existing code...
package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "mime"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // get filename from query
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // basic sanity checks to avoid obvious traversal attempts
    if strings.Contains(fname, "..") || strings.HasPrefix(fname, "/") || strings.HasPrefix(fname, `\`) {
        http.Error(w, "invalid file parameter", http.StatusBadRequest)
        return
    }

    baseDir := "images"

    // clean and join path, then resolve absolute paths to ensure file is inside baseDir
    cleanName := filepath.Clean(fname)
    target := filepath.Join(baseDir, cleanName)

    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // ensure the resolved target is within the base directory
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // check file exists and is not a directory
    info, err := os.Stat(absTarget)
    if err != nil || info.IsDir() {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    // set appropriate content-type if known
    if ctype := mime.TypeByExtension(filepath.Ext(absTarget)); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    http.ServeFile(w, r, absTarget)
}
// ...existing code...
