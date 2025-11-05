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

// Security Note: Always serve over HTTPS in production to protect data in transit.
// Do not run as root; use least privilege for the process user.

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // Security: ListenAndServeTLS should be used in production for HTTPS.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Security: Do not leak internal errors to clients.
        fmt.Fprintf(os.Stderr, "Server error: %v\n", err)
    }
}

/*
    uploadPDFHandler securely handles PDF uploads:
    - Only allows files with .pdf extension and valid PDF magic bytes.
    - Limits file size to prevent DoS attacks.
    - Sanitizes file names and stores files in ./uploads.
    - Does not leak internal errors to the client.
*/
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB
    const uploadPath = "./uploads"

    // Only accept POST requests
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)
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

    // Validate file extension (defense-in-depth)
    if !strings.EqualFold(filepath.Ext(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Generate a random file name to prevent path traversal and collisions
    safeFileName, err := generateSafeFileName(".pdf")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure upload directory exists with secure permissions
    if err := os.MkdirAll(uploadPath, 0700); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Create destination file with least privilege
    dstPath := filepath.Join(uploadPath, safeFileName)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Validate PDF magic bytes (first 4 bytes should be "%PDF")
    if err := validatePDFMagicBytes(file); err != nil {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Copy file content with size limit
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written > maxUploadSize {
        http.Error(w, "File too large or upload failed", http.StatusBadRequest)
        _ = os.Remove(dstPath) // Clean up
        return
    }

    // Security: Do not reveal file path or internal details to client
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully"))
}

// generateSafeFileName creates a random, unguessable file name with the given extension.
func generateSafeFileName(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return fmt.Sprintf("%d_%s%s", time.Now().UnixNano(), hex.EncodeToString(b), ext), nil
}

// validatePDFMagicBytes checks that the file starts with "%PDF".
func validatePDFMagicBytes(file multipart.File) error {
    const magic = "%PDF"
    buf := make([]byte, len(magic))
    n, err := file.Read(buf)
    if err != nil || n != len(magic) {
        return errors.New("unable to read file header")
    }
    if string(buf) != magic {
        return errors.New("invalid PDF magic bytes")
    }
    // Reset file pointer for subsequent reads
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        return errors.New("unable to reset file pointer")
    }
    return nil
}
