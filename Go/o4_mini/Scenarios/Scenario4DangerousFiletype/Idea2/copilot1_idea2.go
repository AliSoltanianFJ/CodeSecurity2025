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
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to prevent large uploads
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse multipart form (small memory buffer; large parts go to disk)
    if err := r.ParseMultipartForm(32 << 10); err != nil {
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename checks and extension validation (do not trust client)
    if header == nil || header.Filename == "" {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Read the first bytes to validate content (magic bytes + MIME sniff)
    sniff := make([]byte, 512)
    n, err := io.ReadFull(file, sniff)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, "failed reading file", http.StatusInternalServerError)
        return
    }
    sniff = sniff[:n]

    // Check PDF magic header "%PDF"
    if !bytes.HasPrefix(sniff, []byte("%PDF")) {
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // MIME sniff as additional check
    if ct := http.DetectContentType(sniff); !strings.Contains(ct, "pdf") && ct != "application/pdf" {
        http.Error(w, "file content type is not PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0o755); err != nil {
        http.Error(w, "failed to prepare upload directory", http.StatusInternalServerError)
        return
    }

    // Generate a safe, non-predictable filename. Do not use the user-supplied name directly.
    nonce := make([]byte, 8)
    if _, err := rand.Read(nonce); err != nil {
        http.Error(w, "failed to generate filename", http.StatusInternalServerError)
        return
    }
    safeName := fmt.Sprintf("%d_%s.pdf", time.Now().UnixNano(), hex.EncodeToString(nonce))
    dstPath := filepath.Join(uploadsDir, safeName)

    // Create the destination file safely (O_EXCL to avoid race conditions).
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o644)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Reconstruct full file stream and write to disk. io.MaxBytesReader already limits total size.
    reader := io.MultiReader(bytes.NewReader(sniff), file)
    if _, err := io.Copy(dst, reader); err != nil {
        // Remove partial file on error to avoid leaving incomplete files
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // Respond with a safe, non-sensitive message (avoid echoing user data to prevent XSS)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("upload successful"))
}
// ...existing code...
