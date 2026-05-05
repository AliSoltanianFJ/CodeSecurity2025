package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const maxUploadSize = 10 << 20 // 10 MiB

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

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    file, _, err := r.FormFile("pdf")
    if err != nil {
        if strings.Contains(err.Error(), "request body too large") {
            http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
            return
        }
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read signature bytes to verify PDF magic
    sig := make([]byte, 5)
    n, err := io.ReadFull(file, sig)
    if err != nil {
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }
    if string(sig[:n]) != "%PDF-" {
        http.Error(w, "invalid file type", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll("./uploads", 0700); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // generate a safe random filename with .pdf extension
    id := make([]byte, 16)
    if _, err := rand.Read(id); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(id) + ".pdf"
    dstPath := filepath.Join("uploads", filename)

    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // prepend the already-read signature back and copy the remainder, enforcing overall size
    reader := io.MultiReader(bytes.NewReader(sig[:n]), file)
    if _, err := io.Copy(dst, io.LimitReader(reader, maxUploadSize)); err != nil {
        http.Error(w, "invalid upload", http.StatusBadRequest)
        // best effort cleanup
        _ = os.Remove(dstPath)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
