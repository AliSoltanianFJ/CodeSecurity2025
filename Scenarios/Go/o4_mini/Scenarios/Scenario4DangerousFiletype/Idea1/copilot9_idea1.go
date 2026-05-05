package main

import (
    "bytes"
    "io"
    "net/http"
    "os"
    "path/filepath"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const maxUploadSize = 10 << 20 // 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // quick magic-bytes check for PDF ("%PDF-")
    head := make([]byte, 512)
    n, err := file.Read(head)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(head[:n], []byte("%PDF-")) {
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }

    // ensure upload directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // create a safe, unique destination file (preserve .pdf extension)
    dst, err := os.CreateTemp(uploadDir, "upload-*.pdf")
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the bytes we already read, then copy the remainder
    if _, err := dst.Write(head[:n]); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // make sure file is world-readable
    _ = dst.Chmod(0o644)

    // optional: log original filename (sanitized) or return minimal confirmation
    _ = filepath.Base(header.Filename)
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded"))
}
// ...existing code...
