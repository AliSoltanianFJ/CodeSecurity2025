package main

import (
    "net/http"
    "path/filepath"
    "strings"
    "os"
    "io"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Prevent path traversal by cleaning and checking the path
    cleanFilename := filepath.Clean(filename)
    if strings.Contains(cleanFilename, "..") || strings.HasPrefix(cleanFilename, "/") || strings.HasPrefix(cleanFilename, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    imgPath := filepath.Join("images", cleanFilename)
    f, err := os.Open(imgPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Optionally set content-type based on file extension
    http.ServeContent(w, r, cleanFilename, fStat(f), f)
}

// Helper to get file info for ServeContent
func fStat(f *os.File) (fi os.FileInfo) {
    fi, _ = f.Stat()
    return
}
