package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

func img(w http.ResponseWriter, r *http.Request) {
    // 1) only GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) get the filename
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // 3) reject any path separators to prevent directory traversal
    if strings.ContainsAny(fname, `/\`) {
        http.Error(w, "Invalid file parameter", http.StatusBadRequest)
        return
    }

    // 4) allow only a small set of safe extensions
    ext := strings.ToLower(filepath.Ext(fname))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".webp":
        // ok
    default:
        http.Error(w, "Invalid file type", http.StatusBadRequest)
        return
    }

    // 5) build the path under your images directory
    imagesDir := "images"
    imagePath := filepath.Join(imagesDir, fname)

    // 6) make sure the file really lives under imagesDir
    absImagePath, err := filepath.Abs(imagePath)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absBaseDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    // On Windows filepath.Abs might return backslashes, so we add the OS-specific separator
    if !strings.HasPrefix(absImagePath, absBaseDir+string(os.PathSeparator)) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // 7) make sure it exists and is not a directory
    info, err := os.Stat(absImagePath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "Invalid file", http.StatusBadRequest)
        return
    }

    // 8) add caching headers (optional but recommended)
    w.Header().Set("Cache-Control", "public, max-age=86400") // cache for 1 day

    // 9) serve the file (sets Content-Type, supports range requests, If-Modified-Since, etc.)
    http.ServeFile(w, r, absImagePath)
}