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
    // get file name from query parameter "file"
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // disallow path separators to prevent traversal attempts
    if strings.ContainsAny(fname, `/\`) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // normalize and take base
    fname = filepath.Base(filepath.Clean(fname))

    // allow only specific image extensions
    ext := strings.ToLower(filepath.Ext(fname))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
        // allowed
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    imgDir := "images"
    fullPath := filepath.Join(imgDir, fname)

    // ensure the final absolute path is inside the images directory
    absImgDir, err := filepath.Abs(imgDir)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    absFull, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // allow exact match or prefix with separator
    if absFull != absImgDir && !strings.HasPrefix(absFull, absImgDir+string(os.PathSeparator)) {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // check file exists and is not a directory
    info, err := os.Stat(absFull)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // set Content-Type and serve file
    ctype := mime.TypeByExtension(ext)
    if ctype == "" {
        ctype = "application/octet-stream"
    }
    w.Header().Set("Content-Type", ctype)
    http.ServeFile(w, r, absFull)
}
