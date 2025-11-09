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

// ...existing code...
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    // enforce overall request size limit
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512)

    // parse multipart form within allowed size
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "file too large or invalid form", http.StatusRequestEntityTooLarge)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read initial bytes to validate file magic and content type
    sniff := make([]byte, 512)
    n, err := file.Read(sniff)
    if err != nil && err != io.EOF {
        http.Error(w, "unable to read file", http.StatusInternalServerError)
        return
    }
    if n < 4 || !bytes.HasPrefix(sniff[:n], []byte("%PDF")) {
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }
    // optional detection (not relied on exclusively)
    ct := http.DetectContentType(sniff[:n])
    if !strings.HasPrefix(ct, "application/") && ct != "application/octet-stream" {
        // don't reveal internals; still allow based on magic bytes
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // create a secure, non-guessable filename (do not use user-provided name)
    random := make([]byte, 16)
    if _, err := rand.Read(random); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(random) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // open destination file with O_EXCL to avoid races/overwrites
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0o644)
    if err != nil {
        http.Error(w, "cannot save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the sniffed bytes, then copy the rest while respecting the size limit
    if _, err := dst.Write(sniff[:n]); err != nil {
        os.Remove(dstPath)
        http.Error(w, "cannot save file", http.StatusInternalServerError)
        return
    }

    // copy remaining bytes up to the max upload size
    limitedReader := io.LimitReader(file, maxUploadSize)
    if _, err := io.Copy(dst, limitedReader); err != nil {
        dst.Close()
        os.Remove(dstPath)
        http.Error(w, "cannot save file", http.StatusInternalServerError)
        return
    }

    // Success: do not expose internal paths or original filename
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "upload successful")
}
// ...existing code...
