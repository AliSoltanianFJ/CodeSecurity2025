package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
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

    // Prevent path traversal by cleaning and joining with images directory
    cleanFilename := filepath.Clean(filename)
    imagePath := filepath.Join("images", cleanFilename)

    // Ensure the final path is still within the images directory
    absImageDir, err := filepath.Abs("images")
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absImagePath, err := filepath.Abs(imagePath)
    if err != nil || len(absImagePath) < len(absImageDir) || absImagePath[:len(absImageDir)] != absImageDir {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    file, err := os.Open(absImagePath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Optionally set content-type based on file extension
    http.ServeContent(w, r, cleanFilename, fileStatModTime(file), file)
}

// Helper to get file mod time for ServeContent
func fileStatModTime(file *os.File) (modTime time.Time) {
    info, err := file.Stat()
    if err == nil {
        modTime = info.ModTime()
    }
    return
}
