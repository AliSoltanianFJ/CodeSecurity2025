package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
    "time"
)

const maxUploadSize = 10 << 20 // 10 MB
const uploadDir = "./uploads"

var validName = regexp.MustCompile(`^[a-zA-Z0-9._-]+$`)

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
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    fn := filepath.Base(header.Filename)
    fn = strings.ToLower(strings.TrimSpace(fn))
    if fn == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    if !strings.HasSuffix(fn, ".pdf") {
        http.Error(w, "only pdf allowed", http.StatusBadRequest)
        return
    }
    namePart := strings.TrimSuffix(fn, ".pdf")
    if !validName.MatchString(namePart) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if !bytes.Contains(buf[:n], []byte("%PDF-")) {
        http.Error(w, "invalid pdf", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    rnd := make([]byte, 8)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    dstName := namePart + "-" + hex.EncodeToString(rnd) + ".pdf"
    dstPath := filepath.Join(uploadDir, dstName)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, io.MultiReader(bytes.NewReader(buf[:n]), file)); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("uploaded"))
}
