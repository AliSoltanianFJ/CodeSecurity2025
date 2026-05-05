package main

import (
    "errors"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20) // 10MB max

    err := r.ParseMultipartForm(10 << 20) // 10MB max memory
    if err != nil {
        http.Error(w, "File too large or invalid form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Sanitize filename
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }
    if !isSafeFilename(filename) {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Create uploads directory if not exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Prevent overwriting files
    dstPath := filepath.Join(uploadDir, filename)
    if _, err := os.Stat(dstPath); err == nil {
        http.Error(w, "File already exists", http.StatusConflict)
        return
    }

    // Save file securely
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Limit the size of the file to 10MB
    written, err := io.Copy(dst, io.LimitReader(file, 10<<20))
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == 0 {
        http.Error(w, "Empty file", http.StatusBadRequest)
        return
    }

    // Optionally, check PDF magic number (first bytes)
    if !isPDF(dstPath) {
        os.Remove(dstPath)
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}

// isSafeFilename checks for dangerous characters in filenames
func isSafeFilename(name string) bool {
    if strings.ContainsAny(name, `\/:*?"<>|`) {
        return false
    }
    if strings.Contains(name, "..") {
        return false
    }
    return true
}

// isPDF checks if the file starts with the PDF magic number
func isPDF(path string) bool {
    f, err := os.Open(path)
    if err != nil {
        return false
    }
    defer f.Close()
    buf := make([]byte, 5)
    _, err = f.Read(buf)
    if err != nil {
        return false
    }
    return string(buf) == "%PDF-"
}
