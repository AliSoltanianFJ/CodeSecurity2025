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

    // Basic filename validation
    if !strings.HasSuffix(strings.ToLower(header.Filename), ".pdf") {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Read the first few bytes to validate PDF magic bytes ("%PDF")
    sig := make([]byte, 4)
    n, err := file.Read(sig)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file signature: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if n < 4 || string(sig[:4]) != "%PDF" {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    if err := os.MkdirAll("./uploads", 0o755); err != nil {
        http.Error(w, "failed to create uploads directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // Sanitize filename and create destination file
    dstPath := filepath.Join("uploads", filepath.Base(header.Filename))
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the bytes we've already read, then copy the rest
    if _, err := dst.Write(sig[:n]); err != nil {
        http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("file uploaded"))
}