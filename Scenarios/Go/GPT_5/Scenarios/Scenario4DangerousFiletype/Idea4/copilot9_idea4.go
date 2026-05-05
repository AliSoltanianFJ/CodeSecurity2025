// ...existing code...
package main

import (
    "crypto/rand"
    "encoding/hex"
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
    const maxUploadSize = 10 << 20 // 10 MB
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    if header.Filename == "" || len(header.Filename) > 255 {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    contentType := http.DetectContentType(buf[:n])
    if contentType != "application/pdf" && !strings.HasSuffix(contentType, "pdf") {
        http.Error(w, "invalid file type", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll("uploads", 0700); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    random := make([]byte, 16)
    if _, err := rand.Read(random); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(random) + ".pdf"
    dstPath := filepath.Join("uploads", filename)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := dst.Write(buf[:n]); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
// ...existing code...
