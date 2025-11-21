package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
)

const (
    maxUploadSize = 10 << 20 // 10 MB
    uploadDir     = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    header := make([]byte, 4)
    n, err := io.ReadFull(file, header)
    if err != nil || n < 4 {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if string(header) != "%PDF" {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    dstName := hex.EncodeToString(randBytes) + ".pdf"
    dstPath := filepath.Join(uploadDir, dstName)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0o644)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    reader := io.MultiReader(bytes.NewReader(header[:n]), file)
    if _, err := io.Copy(dst, reader); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        _ = os.Remove(dstPath)
        return
    }

    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("uploaded"))
}
```// filepath:
