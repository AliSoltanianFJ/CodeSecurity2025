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
// Security: Do not leak internal errors to the client. Log errors securely if needed (not shown here).
// Security: Use HTTPS in production to protect data in transit (not shown here).

const (
    uploadDir        = "./uploads"
    maxUploadSize    = 10 << 20 // 10 MB
    maxFileNameLen   = 100
    pdfMagicBytesLen = 5
)

func main() {
    // Security: In production, use HTTPS and set secure server options.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // Security: ListenAndServeTLS should be used in production.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Log error securely (not shown here)
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow POST method.
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "File too large or invalid form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid file upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Security: Validate file extension and content type.
    if !isPDF(header) {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Security: Check magic bytes to confirm PDF file.
    if !hasPDFMagicBytes(file) {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }
    // Reset file pointer after reading magic bytes.
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Sanitize and randomize filename to prevent path traversal and collisions.
    safeFileName, err := generateSafeFileName(header.Filename)
    if err != nil {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Security: Ensure upload directory exists with least privilege.
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadDir, safeFileName)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the size of the file being written.
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written > maxUploadSize {
        http.Error(w, "File too large", http.StatusBadRequest)
        return
    }

    // Security: Do not reveal file path or sensitive info in response.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "PDF uploaded successfully")
}

// isPDF checks the file extension and MIME type for PDF.
func isPDF(header *multipart.FileHeader) bool {
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        return false
    }
    // Security: Check Content-Type header for PDF.
    contentType := header.Header.Get("Content-Type")
    return contentType == "application/pdf"
}

// hasPDFMagicBytes checks the first bytes for PDF signature ("%PDF-").
func hasPDFMagicBytes(file multipart.File) bool {
    buf := make([]byte, pdfMagicBytesLen)
    n, err := file.Read(buf)
    if err != nil || n < pdfMagicBytesLen {
        return false
    }
    return string(buf) == "%PDF-"
}

// generateSafeFileName sanitizes and randomizes the filename.
func generateSafeFileName(original string) (string, error) {
    // Security: Remove path, limit length, allow only safe characters.
    base := filepath.Base(original)
    base = strings.Map(func(r rune) rune {
        if (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') ||
            r == '.' || r == '_' || r == '-' {
            return r
        }
        return '_'
    }, base)
    if len(base) > maxFileNameLen {
        base = base[:maxFileNameLen]
    }
    if !strings.HasSuffix(strings.ToLower(base), ".pdf") {
        return "", errors.New("invalid file extension")
    }
    // Security: Add random suffix to prevent collisions.
    randBytes := make([]byte, 8)
    if _, err := rand.Read(randBytes); err != nil {
        return "", err
    }
    timestamp := time.Now().UTC().Format("20060102T150405")
    return fmt.Sprintf("%s_%s_%s.pdf",
        strings.TrimSuffix(base, ".pdf"),
        timestamp,
        hex.EncodeToString(randBytes)), nil
}
