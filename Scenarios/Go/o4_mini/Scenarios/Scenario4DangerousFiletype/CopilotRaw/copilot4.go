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
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form (optional size limit)
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "failed to parse multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename sanitization
    filename := filepath.Base(header.Filename)
    if filename == "." || filename == string(filepath.Separator) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "only pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        http.Error(w, "unable to create uploads directory", http.StatusInternalServerError)
        return
    }

    // Create a collision-resistant filename
    safeName := fmt.Sprintf("%d_%s", time.Now().UnixNano(), filename)
    dstPath := filepath.Join(uploadsDir, safeName)

    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "unable to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded"))
}
// ...existing code...
