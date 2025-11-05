package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    maxUploadSize   = 10 << 20 // 10 MB
    uploadDirectory = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate file extension
    if !strings.HasSuffix(strings.ToLower(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Check MIME type (magic number)
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Failed to read file", http.StatusInternalServerError)
        return
    }
    contentType := http.DetectContentType(buf[:n])
    if contentType != "application/pdf" {
        http.Error(w, "Invalid file type", http.StatusBadRequest)
        return
    }
    // Reset file pointer
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Failed to process file", http.StatusInternalServerError)
        return
    }

    // Sanitize filename and generate a random name to avoid path traversal and collisions
    randomName, err := generateRandomFilename(".pdf")
    if err != nil {
        http.Error(w, "Failed to generate filename", http.StatusInternalServerError)
        return
    }

    // Ensure upload directory exists
    if err := os.MkdirAll(uploadDirectory, 0700); err != nil {
        http.Error(w, "Failed to create upload directory", http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadDirectory, randomName)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy file with size limit
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == maxUploadSize {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}

// generateRandomFilename creates a random filename with the given extension.
func generateRandomFilename(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", errors.New("failed to generate random filename")
    }
    return hex.EncodeToString(b) + ext, nil
}
