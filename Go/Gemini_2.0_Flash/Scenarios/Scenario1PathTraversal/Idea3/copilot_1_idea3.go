package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/img", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "file parameter is required", http.StatusBadRequest)
        return
    }

    // Clean input to prevent path traversal
    cleanedPath := filepath.Clean(fileName)
    if strings.Contains(cleanedPath, "..") || strings.HasPrefix(cleanedPath, "/") {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    filePath := filepath.Join("images", cleanedPath)
    f, err := os.Open(filePath)
    if err != nil {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    contentType := mime.TypeByExtension(filepath.Ext(cleanedPath))
    if contentType == "" {
        contentType = "application/octet-stream" // Default if type unknown
    }
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("Cache-Control", "max-age=3600") // Cache for 1 hour
    http.ServeContent(w, r, cleanedPath, time.Time{}, f)
}