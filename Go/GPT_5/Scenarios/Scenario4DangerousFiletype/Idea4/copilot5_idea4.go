package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const maxUploadSize = 10 << 20 // 10 MiB

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    defer file.Close()

    if header == nil || header.Filename == "" {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    if len(header.Filename) > 255 {
        http.Error(w, "filename too long", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "only pdf files are allowed", http.StatusBadRequest)
        return
    }

    buf := make([]byte, 512)
    n, err := io.ReadFull(file, buf)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(buf[:n], []byte("%PDF-")) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0o700); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(randBytes) + ".pdf"
    dstPath := filepath.Join(uploadsDir, filename)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0o600)
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
    w.Write([]byte("uploaded"))
}
