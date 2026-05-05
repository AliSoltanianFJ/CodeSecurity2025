package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

const maxUploadSize = 10 << 20 // 10 MB
const uploadDir = "./uploads"

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}
// ...existing code...

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    ct := r.Header.Get("Content-Type")
    if ct == "" || !strings.Contains(ct, "multipart/form-data") {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large or malformed", http.StatusRequestEntityTooLarge)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    sniff := make([]byte, 512)
    n, _ := file.Read(sniff)
    if !bytes.HasPrefix(sniff[:n], []byte("%PDF")) {
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    randBytes := make([]byte, 8)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    filename := fmt.Sprintf("%d-%s.pdf", time.Now().UnixNano(), hex.EncodeToString(randBytes))
    dstPath := filepath.Join(uploadDir, filename)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, io.MultiReader(bytes.NewReader(sniff[:n]), file)); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded"))
}
// ...existing code...
