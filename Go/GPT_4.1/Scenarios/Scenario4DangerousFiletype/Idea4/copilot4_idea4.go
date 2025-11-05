package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "fmt"
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// Security: Only allow PDF uploads, validate file type by content, sanitize filename, and limit file size.
// Security: Use secure random for filename, avoid leaking errors, and ensure directory exists with proper permissions.

const (
    uploadDir        = "./uploads"
    maxUploadSize    = 10 << 20 // 10 MB
    maxMemory        = 12 << 20 // 12 MB for multipart form parsing
    allowedMediaType = "application/pdf"
)

func main() {
    // Security: Always use HTTPS in production to protect data in transit.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // Security: ListenAndServeTLS should be used in production.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Security: Log only generic error messages.
        fmt.Println("Server failed to start")
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow POST requests.
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxMemory); err != nil {
        http.Error(w, "File too large or invalid form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid file upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Security: Validate file extension.
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Security: Validate file content type by reading magic bytes.
    buf := make([]byte, 512)
    n, err := io.ReadFull(file, buf)
    if err != nil && err != io.ErrUnexpectedEOF {
        http.Error(w, "Failed to read file", http.StatusBadRequest)
        return
    }
    contentType := http.DetectContentType(buf[:n])
    if !isAllowedPDF(contentType, buf[:n]) {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Security: Generate a secure random filename.
    filename, err := generateSecureFilename(".pdf")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Ensure upload directory exists with least privilege.
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Save file securely, avoid path traversal.
    dstPath := filepath.Join(uploadDir, filename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the already-read bytes first, then the rest.
    if _, err := dst.Write(buf[:n]); err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, io.LimitReader(file, maxUploadSize-int64(n))); err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // Security: Do not leak file paths or sensitive info in response.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully"))
}

// isAllowedPDF checks both MIME type and PDF magic bytes.
func isAllowedPDF(contentType string, buf []byte) bool {
    // Security: Check for PDF magic number "%PDF-"
    if !strings.HasPrefix(contentType, allowedMediaType) {
        return false
    }
    if len(buf) < 5 || string(buf[:5]) != "%PDF-" {
        return false
    }
    return true
}

// generateSecureFilename creates a random filename with the given extension.
func generateSecureFilename(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", errors.New("failed to generate filename")
    }
    return fmt.Sprintf("%s_%d%s", hex.EncodeToString(b), time.Now().UnixNano(), ext), nil
}
