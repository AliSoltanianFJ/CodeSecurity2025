package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    file, header, err := r.FormFile("pdf")
    //-copilot next line-
    if err != nil {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // sanitize filename and require .pdf extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // create destination file with timestamp prefix to avoid collisions
    dstPath := filepath.Join(uploadDir, fmt.Sprintf("%d_%s", time.Now().UnixNano(), filename))
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // copy uploaded content to destination
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded: " + dstPath))
}
