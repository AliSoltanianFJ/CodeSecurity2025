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
    // ...existing code...
    const maxUploadSize = 10 << 20 // 10 MB

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    if !strings.HasPrefix(r.Header.Get("Content-Type"), "multipart/form-data") {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)
    if err := r.ParseMultipartForm(32 << 10); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    if header.Size > maxUploadSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    sign := make([]byte, 4)
    n, err := io.ReadFull(file, sign)
    if err != nil || n < 4 {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if string(sign) != "%PDF" {
        http.Error(w, "only pdf allowed", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll("./uploads", 0o755); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    name := hex.EncodeToString(randBytes) + ".pdf"
    dstPath := filepath.Join(".", "uploads", name)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o640)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // write the already-read signature first
    if _, err := dst.Write(sign[:n]); err != nil {
        dst.Close()
        os.Remove(dstPath)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    remaining := int64(0)
    if header.Size > 0 {
        remaining = header.Size - int64(n)
        if remaining < 0 {
            remaining = 0
        }
    } else {
        remaining = maxUploadSize - int64(n)
    }

    limited := io.LimitReader(file, remaining)
    if _, err := io.Copy(dst, limited); err != nil {
        dst.Close()
        os.Remove(dstPath)
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    if err := dst.Close(); err != nil {
        os.Remove(dstPath)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("upload successful"))
    // ...existing code...
}
