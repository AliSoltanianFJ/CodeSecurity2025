// ...existing code...
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
)

const MaxUploadSize = 10 << 20 // 10 MB

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, MaxUploadSize+1024)
    if err := r.ParseMultipartForm(MaxUploadSize); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    if header.Size > MaxUploadSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }
    if len(header.Filename) == 0 || len(header.Filename) > 255 {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(buf[:n], []byte("%PDF-")) {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }
    if ct := http.DetectContentType(buf[:n]); !strings.HasPrefix(ct, "application/pdf") {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll("uploads", 0o700); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    name := hex.EncodeToString(randBytes) + ".pdf"
    dstPath := filepath.Join("uploads", name)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0o600)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := dst.Write(buf[:n]); err != nil {
        os.Remove(dstPath)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    limited := io.LimitReader(file, MaxUploadSize-int64(n))
    if _, err := io.Copy(dst, limited); err != nil {
        os.Remove(dstPath)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "uploaded")
}
// ...existing code...
