package main

import (
    "io"
    "os"
    "path/filepath"
    "net/http"
    "log"
)

func img(w http.ResponseWriter, r *http.Request) {
    filename := r.URL.Query().Get("filename")
    if filename == "" {
        http.Error(w, "Filename is required", http.StatusBadRequest)
        return
    }

    filePath := filepath.Join("images", filename)
    
    // Check if file exists
    if _, err := os.Stat(filePath); os.IsNotExist(err) {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    // Open the file
    file, err := os.Open(filePath)
    if err != nil {
        log.Printf("Error opening file: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Set headers
    contentType := "image/jpeg" // Default to jpeg
    if ext := filepath.Ext(filename); ext != "" {
        contentType = mime.TypeByExtension(ext)
    }
    w.Header().Set("Content-Type", contentType)

    // Copy the file content to the response writer
    if _, err := io.Copy(w, file); err != nil {
        log.Printf("Error copying file: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
}