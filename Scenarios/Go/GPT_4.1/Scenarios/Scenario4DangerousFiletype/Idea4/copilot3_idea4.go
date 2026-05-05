package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// Security: Only allow PDF files, max 10MB, random file names, and sanitize all inputs.
// Ensure uploads directory exists with least privilege (0700).
const (
    maxUploadSize    = 10 << 20 // 10 MB
    uploadPath       = "./uploads"
    allowedExtension = ".pdf"
)

func main() {
    // Security: Always use HTTPS in production to protect data in transit.
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    fmt.Println("Listening on http://localhost:8080 (use HTTPS in production)")
    http.ListenAndServe(":8080", nil)
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
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
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
    if ext != allowedExtension {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Security: Check MIME type by reading file header.
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Failed to read file", http.StatusBadRequest)
        return
    }
    contentType := http.DetectContentType(buf[:n])
    if contentType != "application/pdf" && contentType != "application/octet-stream" {
        http.Error(w, "Invalid file type", http.StatusBadRequest)
        return
    }
    // Reset file pointer after sniffing.
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Failed to process file", http.StatusInternalServerError)
        return
    }

    // Security: Generate a random file name to prevent path traversal and collisions.
    randomName, err := generateRandomFilename(16, allowedExtension)
    if err != nil {
        http.Error(w, "Failed to generate file name", http.StatusInternalServerError)
        return
    }

    // Security: Ensure upload directory exists with least privilege.
    if err := os.MkdirAll(uploadPath, 0700); err != nil {
        http.Error(w, "Failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // Security: Create file with least privilege (0600).
    dstPath := filepath.Join(uploadPath, randomName)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the size of the copied file.
    written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written > maxUploadSize {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        os.Remove(dstPath)
        return
    }

    // Security: Do not leak file paths or sensitive info in response.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}

// generateRandomFilename generates a random filename with the given length and extension.
// Security: Prevents filename guessing and path traversal.
func generateRandomFilename(length int, ext string) (string, error) {
    if length <= 0 || length > 64 {
        return "", errors.New("invalid filename length")
    }
    b := make([]byte, length)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b) + ext, nil
}
