// ...existing code...
package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // only allow POST uploads
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
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadDir, filename)
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded"))
}
// ...existing code...
