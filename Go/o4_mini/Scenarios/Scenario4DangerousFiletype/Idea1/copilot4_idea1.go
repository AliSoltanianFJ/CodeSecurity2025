package main

import (
    "bytes"
    "fmt"
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
    const maxUploadSize = 10 << 20 // 10 MB

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // enforce max request size
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid multipart form or file too large", http.StatusBadRequest)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read initial bytes to validate PDF magic/header
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }

    // check PDF signature "%PDF-"; fallback to MIME sniffing
    if n < 5 || string(buf[:5]) != "%PDF-" {
        if ct := http.DetectContentType(buf[:n]); ct != "application/pdf" {
            http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
            return
        }
    }

    // prepare reader: reset if possible, otherwise prepend the bytes read
    var reader io.Reader
    if seeker, ok := file.(io.Seeker); ok {
        if _, err := seeker.Seek(0, io.SeekStart); err != nil {
            http.Error(w, "failed to process uploaded file", http.StatusInternalServerError)
            return
        }
        reader = file
    } else {
        reader = io.MultiReader(bytes.NewReader(buf[:n]), file)
    }

    // ensure upload directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // create a unique file in the uploads directory; do not use the client filename directly
    dst, err := os.CreateTemp(uploadDir, "upload-*.pdf")
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, reader); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // respond with the stored filename (not the full path)
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, filepath.Base(dst.Name()))
}
