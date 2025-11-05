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

    // Limit request body size to prevent DoS attacks (e.g., 10MB)
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    err := r.ParseMultipartForm(10 << 20) // 10MB max memory
    if err != nil {
        http.Error(w, "File too large or invalid form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate file extension
    if !strings.HasSuffix(strings.ToLower(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Read the first 4 bytes to check PDF magic number
    buf := make([]byte, 4)
    _, err = io.ReadFull(file, buf)
    if err != nil {
        http.Error(w, "Failed to read file", http.StatusBadRequest)
        return
    }
    if string(buf) != "%PDF" {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Reset file pointer to start
    _, err = file.Seek(0, io.SeekStart)
    if err != nil {
        http.Error(w, "Failed to process file", http.StatusInternalServerError)
        return
    }

    // Create uploads directory if not exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Generate a random filename to avoid path traversal and collisions
    randomName, err := randomHex(16)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    safeFilename := randomName + ".pdf"
    dstPath := filepath.Join(uploadDir, safeFilename)

    // Save the file securely
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Limit the size of the file to 10MB
    written, err := io.Copy(dst, io.LimitReader(file, 10<<20))
	if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == 10<<20 {
        http.Error(w, "File too large", http.StatusBadRequest)
        os.Remove(dstPath)
        return
    }

    // Respond with success (do not reveal file path)
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully."))
}

// randomHex generates a random hex string of n bytes.
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", errors.New("failed to generate random filename")
    }
    return hex.EncodeToString(b), nil
}
