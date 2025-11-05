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
    maxUploadSize = 10 << 20 // 10 MB
    uploadPath    = "./uploads"
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
    if !strings.EqualFold(filepath.Ext(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Read first 4 bytes to check PDF magic number
    buf := make([]byte, 4)
    if _, err := io.ReadFull(file, buf); err != nil {
        http.Error(w, "Failed to read file", http.StatusBadRequest)
        return
    }
    if string(buf) != "%PDF" {
        http.Error(w, "File is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Reset file pointer to start
    if seeker, ok := file.(io.Seeker); ok {
        seeker.Seek(0, io.SeekStart)
    } else {
        http.Error(w, "Cannot process file", http.StatusInternalServerError)
        return
    }

    // Ensure upload directory exists
    if err := os.MkdirAll(uploadPath, 0700); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Generate a random filename to avoid path traversal and collisions
    filename, err := generateSafeFilename(".pdf")
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    dstPath := filepath.Join(uploadPath, filename)

    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy file with size limit
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil || written == 0 {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}

// generateSafeFilename creates a random filename with the given extension
func generateSafeFilename(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", errors.New("failed to generate filename")
    }
    return hex.EncodeToString(b) + ext, nil
}
