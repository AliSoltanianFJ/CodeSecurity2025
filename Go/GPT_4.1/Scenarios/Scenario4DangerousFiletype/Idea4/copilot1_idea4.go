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

// Security: Only allow PDF uploads, validate content type and file signature (magic number).
// Security: Use random file names to prevent path traversal and filename collisions.
// Security: Limit file size to prevent DoS attacks.
// Security: Do not leak internal errors to the client.
// Security: Ensure uploads directory exists with least privilege.

const (
    maxUploadSize    = 10 * 1024 * 1024 // 10 MB
    uploadPath       = "./uploads"
    allowedMimeType  = "application/pdf"
    pdfMagicNumber   = "%PDF-"
    maxFilenameBytes = 16
)

func main() {
    // Security: Always use HTTPS in production to protect data in transit.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // Security: ListenAndServeTLS should be used in production with valid certs.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Log fatal error securely.
        fmt.Fprintf(os.Stderr, "Server failed to start: %v\n", err)
        os.Exit(1)
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only accept POST requests.
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
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

    // Security: Validate file extension.
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Security: Validate MIME type from the header.
    mimeType := header.Header.Get("Content-Type")
    if mimeType != allowedMimeType {
        http.Error(w, "Invalid file type", http.StatusBadRequest)
        return
    }

    // Security: Validate file signature (magic number).
    buf := make([]byte, 5)
    if _, err := io.ReadFull(file, buf); err != nil {
        http.Error(w, "Failed to read file", http.StatusBadRequest)
        return
    }
    if string(buf) != pdfMagicNumber {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Security: Reset file pointer to start for saving.
    if seeker, ok := file.(io.Seeker); ok {
        seeker.Seek(0, io.SeekStart)
    } else {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Generate a random file name to prevent path traversal and collisions.
    randomName, err := generateRandomFilename(".pdf")
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Ensure uploads directory exists with least privilege.
    if err := os.MkdirAll(uploadPath, 0700); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadPath, randomName)

    // Security: Create file with least privilege.
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the size of the file copied.
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written > maxUploadSize {
        http.Error(w, "File too large", http.StatusBadRequest)
        os.Remove(dstPath)
        return
    }

    // Security: Do not return file path or sensitive info to client.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully"))
}

// generateRandomFilename creates a random filename with the given extension.
// Security: Prevents path traversal and filename guessing.
func generateRandomFilename(ext string) (string, error) {
    b := make([]byte, maxFilenameBytes)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b) + ext, nil
}
