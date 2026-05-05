package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "fmt"
    "io"
    "mime/multipart"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// Security: Only allow PDF uploads, validate content type and magic number, sanitize filename, and limit file size.
// Security: Do not leak internal errors to client. Log errors securely if needed (not shown here).
// Security: Principle of least privilege—uploads directory should have minimal permissions.

const (
    maxUploadSize    = 10 << 20 // 10 MB
    uploadPath       = "./uploads"
    pdfMagicBytesLen = 5
)

func main() {
    // Security: Use HTTPS in production to protect data in transit.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // Security: ListenAndServeTLS should be used in production.
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "File too large or invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid file upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Security: Validate file extension.
    if !strings.EqualFold(filepath.Ext(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Security: Validate PDF magic number.
    if err := validatePDFMagic(file); err != nil {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }
    // Reset file pointer after reading magic bytes.
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Sanitize filename and generate a random name to avoid path traversal and collisions.
    safeFilename, err := generateSafeFilename(header.Filename)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Ensure upload directory exists with least privilege.
    if err := os.MkdirAll(uploadPath, 0700); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadPath, safeFilename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the number of bytes copied to prevent large file attacks.
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written == 0 {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // Security: Do not reveal file path or sensitive info to client.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "PDF uploaded successfully")
}

// validatePDFMagic checks the first few bytes for the PDF magic number ("%PDF-").
func validatePDFMagic(file multipart.File) error {
    magic := make([]byte, pdfMagicBytesLen)
    n, err := file.Read(magic)
    if err != nil || n < pdfMagicBytesLen {
        return errors.New("unable to read file header")
    }
    if string(magic) != "%PDF-" {
        return errors.New("not a valid PDF file")
    }
    return nil
}

// generateSafeFilename returns a random, sanitized filename with .pdf extension.
func generateSafeFilename(original string) (string, error) {
    // Security: Remove path and dangerous characters.
    ext := ".pdf"
    randomBytes := make([]byte, 16)
    if _, err := rand.Read(randomBytes); err != nil {
        return "", err
    }
    timestamp := time.Now().UTC().Format("20060102T150405")
    return fmt.Sprintf("%s_%s%s", timestamp, hex.EncodeToString(randomBytes), ext), nil
}
