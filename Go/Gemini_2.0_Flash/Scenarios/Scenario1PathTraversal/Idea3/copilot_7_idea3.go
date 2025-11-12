package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

func img(w http.ResponseWriter, r *http.Request) {
    // Get the filename from the GET parameter
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "No file specified", http.StatusBadRequest)
        return
    }

    // Security: Only allow files with .png, .jpg, .jpeg extensions
    validExtension := false
    for _, ext := range []string{".png", ".jpg", ".jpeg"} {
        if strings.HasSuffix(strings.ToLower(filename), ext) {
            validExtension = true
            break
        }
    }
    if !validExtension {
        http.Error(w, "Invalid file type", http.StatusBadRequest)
        return
    }

    // Security: Prevent path traversal
    cleanFilename := filepath.Clean(filename)
    if strings.Contains(cleanFilename, "..") || strings.HasPrefix(cleanFilename, "/") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Build the absolute path to the image
    baseDir, _ := os.Getwd()
    imgDir := filepath.Join(baseDir, "images")
    imgPath := filepath.Join(imgDir, cleanFilename)

    // Security: Only serve files within the images directory
    absImgPath, _ := filepath.Abs(imgPath)
    if !strings.HasPrefix(absImgPath, imgDir) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the image file
    file, err := os.Open(absImgPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Set the Content-Type header based on file extension
    switch strings.ToLower(filepath.Ext(filename)) {
    case ".png":
        w.Header().Set("Content-Type", "image/png")
    case ".jpg", ".jpeg":
        w.Header().Set("Content-Type", "image/jpeg")
    default:
        // Default to binary data for unknown file types
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    // Use io.Copy to send the file contents to the response writer
    io.Copy(w, file)
}