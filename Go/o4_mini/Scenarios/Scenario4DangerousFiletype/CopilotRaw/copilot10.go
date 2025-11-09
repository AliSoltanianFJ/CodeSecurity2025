package main

import (
    "net/http"
    "os"
    "io"
    "path/filepath"
    "strings"
)

/* ...existing code... */
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit memory for multipart form parsing
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        http.Error(w, "failed to parse multipart form: "+err.Error(), http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // simple check: filename must end with .pdf (case-insensitive)
    filename := header.Filename
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "uploaded file must have .pdf extension", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // sanitize filename and create destination path
    dstPath := filepath.Join(uploadDir, filepath.Base(filename))
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // copy uploaded content to destination file
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("file uploaded successfully"))
}