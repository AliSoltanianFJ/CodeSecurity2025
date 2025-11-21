package main

import (
    "fmt"
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

// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read first 512 bytes to detect content type
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    contentType := http.DetectContentType(buf[:n])
    if !strings.Contains(contentType, "pdf") && contentType != "application/pdf" {
        http.Error(w, "uploaded file is not a PDF (detected: "+contentType+")", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    dstDir := "./uploads"
    if err := os.MkdirAll(dstDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // Sanitize filename and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if filename == "" {
        filename = "upload.pdf"
    }
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        filename += ".pdf"
    }

    dstPath := filepath.Join(dstDir, filename)
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the bytes we already read, then copy the rest
    if n > 0 {
        if _, err := dst.Write(buf[:n]); err != nil {
            http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
            return
        }
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "saved %s\n", dstPath)
}
