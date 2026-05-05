package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    // Allow only POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to avoid memory exhaustion / DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse multipart form (small memory; files streamed to disk if large)
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "failed to parse multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic client-side filename check (do NOT rely on it for security)
    origName := header.Filename
    if origName == "" || !strings.HasSuffix(strings.ToLower(origName), ".pdf") {
        http.Error(w, "invalid file name or extension", http.StatusBadRequest)
        return
    }

    // Read up to 512 bytes to validate magic bytes and content
    var headBuf [512]byte
    n, err := file.Read(headBuf[:])
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file", http.StatusInternalServerError)
        return
    }
    // Check PDF magic header ("%PDF")
    if n < 4 || string(headBuf[:4]) != "%PDF" {
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure upload directory exists with restrictive permissions
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // Generate a secure random filename; do NOT use the user-supplied filename
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "failed to generate filename", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rnd) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // Create destination file with safe permissions and exclusive create to avoid races
    dstFile, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dstFile.Close()

    // Write the bytes already read, then copy the remainder
    if _, err := dstFile.Write(headBuf[:n]); err != nil {
        os.Remove(dstPath)
        http.Error(w, "failed to write file", http.StatusInternalServerError)
        return
    }

    // Copy remainder. MaxBytesReader ensures overall size limit.
    if _, err := io.Copy(dstFile, file); err != nil {
        os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // Respond with the generated filename only (no original filename or other sensitive info)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, filename)
}
