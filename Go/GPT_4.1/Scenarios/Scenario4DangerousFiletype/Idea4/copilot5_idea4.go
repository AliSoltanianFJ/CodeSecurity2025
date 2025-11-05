package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "io"
    "mime/multipart"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// Security-relevant decisions and comments are included throughout this code.

func main() {
    // Always use HTTPS in production to protect data in transit.
    // For demonstration, this uses HTTP. In production, use ListenAndServeTLS.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request body size to prevent DoS attacks (e.g., max 10MB)
    const maxUploadSize = 10 << 20 // 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse multipart form with a reasonable memory limit
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Failed to read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate file extension (defense-in-depth, not sufficient alone)
    if !strings.EqualFold(filepath.Ext(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Validate MIME type by reading the file header (magic bytes)
    if err := validatePDFMagicBytes(file); err != nil {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }
    // Reset file pointer after reading magic bytes
    if seeker, ok := file.(io.Seeker); ok {
        seeker.Seek(0, io.SeekStart)
    } else {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Generate a secure random filename to prevent path traversal and collisions
    filename, err := generateSecureFilename(".pdf")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the uploads directory exists with least privilege
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Create the file with restrictive permissions
    dstPath := filepath.Join(uploadDir, filename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy the uploaded file to the destination, limiting the size
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written == 0 {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // Respond with a generic success message (do not leak file paths)
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully."))
}

// validatePDFMagicBytes checks the first few bytes for the PDF signature ("%PDF-")
func validatePDFMagicBytes(file multipart.File) error {
    const pdfMagic = "%PDF-"
    buf := make([]byte, len(pdfMagic))
    n, err := file.Read(buf)
    if err != nil || n != len(pdfMagic) {
        return errors.New("unable to read file header")
    }
    if string(buf) != pdfMagic {
        return errors.New("file is not a valid PDF")
    }
    return nil
}

// generateSecureFilename creates a random filename with the given extension
func generateSecureFilename(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    timestamp := time.Now().UTC().Format("20060102T150405")
    return timestamp + "_" + hex.EncodeToString(b) + ext, nil
}
