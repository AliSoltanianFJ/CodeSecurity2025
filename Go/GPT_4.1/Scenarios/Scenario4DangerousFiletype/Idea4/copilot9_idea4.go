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

// Security: Only allow PDF uploads, validate file type by content, not just extension.
// Security: Use secure random file names to avoid path traversal and collisions.
// Security: Limit file size to prevent DoS attacks.
// Security: Do not leak internal errors to the client.
// Security: Ensure uploads directory exists and is not world-writable.
// Security: Recommend running behind HTTPS for transport security.

const (
    maxUploadSize    = 10 << 20 // 10 MB
    uploadPath       = "./uploads"
    allowedMimeTypes = "application/pdf"
)

func main() {
    // Security: Use HTTPS in production. This is for demonstration only.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    err := http.ListenAndServe(":8080", nil)
    if err != nil {
        // Log error securely (in production, use a logging framework)
        fmt.Fprintf(os.Stderr, "Server failed to start: %v\n", err)
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow POST requests
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid file upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Security: Validate file type by reading magic bytes
    if err := validatePDF(file); err != nil {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Security: Generate a secure random file name with .pdf extension
    filename, err := generateSecureFilename(".pdf")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Ensure uploads directory exists with correct permissions
    if err := ensureUploadsDir(uploadPath); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Save file securely, do not allow path traversal
    dstPath := filepath.Join(uploadPath, filename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the number of bytes copied to prevent large file attacks
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written > maxUploadSize {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        _ = os.Remove(dstPath) // Cleanup
        return
    }

    // Security: Do not return file path or sensitive info to client
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully"))
}

// validatePDF checks the file's magic bytes and MIME type for PDF
func validatePDF(file multipart.File) error {
    // Security: Read first 512 bytes for content sniffing
    const sniffLen = 512
    buf := make([]byte, sniffLen)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        return err
    }
    // Reset file pointer for later use
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        return err
    }

    // Check for PDF magic number: "%PDF-"
    if !strings.HasPrefix(string(buf), "%PDF-") {
        return errors.New("file is not a valid PDF")
    }

    // Check MIME type
    mimeType := http.DetectContentType(buf[:n])
    if mimeType != allowedMimeTypes {
        return errors.New("invalid MIME type")
    }
    return nil
}

// generateSecureFilename creates a random filename with the given extension
func generateSecureFilename(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b) + ext, nil
}

// ensureUploadsDir creates the uploads directory if it doesn't exist
func ensureUploadsDir(path string) error {
    info, err := os.Stat(path)
    if os.IsNotExist(err) {
        // Security: 0700 so only the owner can read/write/execute
        return os.MkdirAll(path, 0700)
    }
    if err != nil {
        return err
    }
    if !info.IsDir() {
        return errors.New("upload path is not a directory")
    }
    return nil
}
