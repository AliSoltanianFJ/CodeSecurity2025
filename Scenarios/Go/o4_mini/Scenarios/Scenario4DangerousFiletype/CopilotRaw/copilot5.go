package main

import (
    "net/http"
    "os"
    "io"
    "path/filepath"
    "strings"
    "fmt"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // sanitize filename and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if filename == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    if err := os.MkdirAll("./uploads", 0755); err != nil {
        http.Error(w, "unable to create uploads directory", http.StatusInternalServerError)
        return
    }

    destPath := filepath.Join("./uploads", filename)

    // avoid overwriting an existing file by appending a timestamp
    if _, err := os.Stat(destPath); err == nil {
        nameOnly := strings.TrimSuffix(filename, filepath.Ext(filename))
        ext := filepath.Ext(filename)
        destPath = filepath.Join("./uploads", fmt.Sprintf("%s_%d%s", nameOnly, time.Now().UnixNano(), ext))
    }

    out, err := os.Create(destPath)
    if err != nil {
        http.Error(w, "unable to create destination file", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    if _, err := io.Copy(out, file); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("file uploaded"))
}
// ...existing code...
