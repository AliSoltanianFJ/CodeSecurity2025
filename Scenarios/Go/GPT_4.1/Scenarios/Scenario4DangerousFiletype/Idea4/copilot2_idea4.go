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

// Security: Only allow PDF files, validate content type and magic bytes, sanitize filename, and limit file size.
// Security: Do not leak internal errors to the client. Log errors securely if needed (omitted here for brevity).
// Security: Use HTTPS in production to protect data in transit.

const (
    uploadDir         = "./uploads"
    maxUploadSize     = 10 << 20 // 10 MB
    maxFileNameLength = 100
)

// Ensure upload directory exists with least privilege.
func init() {
    err := os.MkdirAll(uploadDir, 0700)
    if err != nil {
        panic("Failed to create upload directory securely")
    }
}

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // Security: In production, use HTTPS and proper TLS configuration.
    err := http.ListenAndServe(":8080", nil)
    if err != nil {
        // Do not leak details in production logs.
        fmt.Println("Server failed to start")
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only accept POST requests.
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

    // Security: Validate file name.
    filename, err := sanitizeFileName(header.Filename)
    if err != nil {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Security: Validate file type by checking magic bytes.
    if err := validatePDF(file); err != nil {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Security: Generate a random file name to prevent path traversal and collisions.
    randomName, err := randomHex(16)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    safePath := filepath.Join(uploadDir, fmt.Sprintf("%s_%s", randomName, filename))

    // Security: Create file with least privilege.
    dst, err := os.OpenFile(safePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Could not save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the size of the file written to disk.
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written > maxUploadSize {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // Security: Do not leak file path or sensitive info.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully"))
}

// sanitizeFileName ensures the file name is safe and does not contain path traversal or dangerous characters.
func sanitizeFileName(name string) (string, error) {
    name = filepath.Base(name)
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > maxFileNameLength {
        return "", errors.New("invalid file name length")
    }
    // Only allow alphanumeric, dash, underscore, and dot.
    for _, c := range name {
        if !(c == '.' || c == '-' || c == '_' ||
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9')) {
            return "", errors.New("invalid character in file name")
        }
    }
    // Only allow .pdf extension (case-insensitive).
    if !strings.HasSuffix(strings.ToLower(name), ".pdf") {
        return "", errors.New("file must have .pdf extension")
    }
    return name, nil
}

// validatePDF checks the file's magic bytes to ensure it's a PDF.
func validatePDF(file multipart.File) error {
    // PDF files start with "%PDF-"
    const pdfMagic = "%PDF-"
    buf := make([]byte, len(pdfMagic))
    _, err := io.ReadFull(file, buf)
    if err != nil {
        return errors.New("could not read file header")
    }
    if string(buf) != pdfMagic {
        return errors.New("file is not a valid PDF")
    }
    // Reset file pointer for further reading.
    _, err = file.Seek(0, io.SeekStart)
    if err != nil {
        return errors.New("could not reset file pointer")
    }
    return nil
}

// randomHex generates a random hex string of the given length.
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    _, err := rand.Read(b)
    if err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
