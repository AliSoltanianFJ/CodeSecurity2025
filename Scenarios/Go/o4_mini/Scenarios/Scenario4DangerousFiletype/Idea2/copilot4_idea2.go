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

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit total request size (including form overhead). 10MB + small margin for form metadata.
    const maxUploadSize = 10 << 20 // 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)

    // Parse multipart form with the same limit
    if err := r.ParseMultipartForm(maxUploadSize + 1024); err != nil {
        http.Error(w, "request too large or malformed", http.StatusRequestEntityTooLarge)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read initial bytes to validate file type (PDF magic bytes)
    const sniffLen = 512
    buf := make([]byte, sniffLen)
    n, err := io.ReadFull(file, buf)
    if err != nil && err != io.EOF && err != io.ErrUnexpectedEOF {
        http.Error(w, "could not read file", http.StatusBadRequest)
        return
    }
    preview := buf[:n]

    // Validate PDF magic header
    if !bytes.HasPrefix(preview, []byte("%PDF-")) {
        // Some broken PDF producers may not strictly include the dash, but magic bytes are standard.
        // Reject if magic is not present to avoid dangerous file types.
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with safe permissions
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a random filename (do NOT use user-supplied filename)
    rn := make([]byte, 16)
    if _, err := rand.Read(rn); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rn) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // Create destination file atomically and with restrictive permissions
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the preview bytes already read, then copy the remainder with limit to avoid oversized writes
    if _, err := dst.Write(preview); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        os.Remove(dstPath)
        return
    }

    // Remaining bytes allowed
    remaining := int64(maxUploadSize) - int64(n)
    if remaining < 0 {
        // already exceeded allowed size
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        os.Remove(dstPath)
        return
    }

    written, err := io.Copy(dst, io.LimitReader(file, remaining))
    if err != nil {
        http.Error(w, "could not save file", http.StatusInternalServerError)
        os.Remove(dstPath)
        return
    }

    // Final size check (n + written <= maxUploadSize)
    if int64(n)+written > maxUploadSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        os.Remove(dstPath)
        return
    }

    // Success: return the generated filename only (do not reveal full server paths)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = io.WriteString(w, strings.TrimSpace(filename))
}
// ...existing code...
