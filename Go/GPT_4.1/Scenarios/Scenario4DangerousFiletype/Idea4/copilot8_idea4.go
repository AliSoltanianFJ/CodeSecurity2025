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

// Security: Only allow PDF files, validate content type and magic number, sanitize filename, and limit file size.
// Security: Ensure uploads directory exists with least privilege.
const (
    uploadDir        = "./uploads"
    maxUploadSize    = 10 << 20 // 10 MB
    maxFilenameLen   = 100
    pdfMagicBytesLen = 5
)

func main() {
    // Security: Always use HTTPS in production. This is for demonstration only.
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

    // Security: Parse multipart form with size limit.
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

    // Security: Validate filename (no path traversal, reasonable length, no special chars).
    filename, err := sanitizeFilename(header.Filename)
    if err != nil {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Security: Check file extension.
    if !strings.EqualFold(filepath.Ext(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Security: Check content type (MIME sniffing).
    buff := make([]byte, pdfMagicBytesLen)
    n, err := io.ReadFull(file, buff)
    if err != nil || n != pdfMagicBytesLen {
        http.Error(w, "Failed to read file", http.StatusBadRequest)
        return
    }
    if !isPDFMagic(buff) {
        http.Error(w, "File is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Security: Reset file pointer to start.
    if seeker, ok := file.(io.Seeker); ok {
        seeker.Seek(0, io.SeekStart)
    } else {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Ensure uploads directory exists with least privilege.
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Security: Generate a random filename to avoid collisions and enumeration.
    randomName, err := randomHex(16)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    safePath := filepath.Join(uploadDir, randomName+".pdf")

    // Security: Create file with least privilege, fail if exists.
    dst, err := os.OpenFile(safePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Security: Limit the number of bytes copied to prevent large file attacks.
    written, err := io.CopyN(dst, file, maxUploadSize)
    if err != nil && !errors.Is(err, io.EOF) {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == maxUploadSize {
        http.Error(w, "File too large", http.StatusBadRequest)
        os.Remove(safePath)
        return
    }

    // Security: Do not leak file path or sensitive info in response.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "PDF uploaded successfully.")
}

// sanitizeFilename ensures the filename is safe and does not allow path traversal or special chars.
func sanitizeFilename(name string) (string, error) {
    name = filepath.Base(name)
    if len(name) > maxFilenameLen {
        return "", errors.New("filename too long")
    }
    if strings.ContainsAny(name, `\/:*?"<>|`) {
        return "", errors.New("invalid characters in filename")
    }
    return name, nil
}

// isPDFMagic checks for the PDF file magic number ("%PDF-").
func isPDFMagic(b []byte) bool {
    return len(b) >= 5 && string(b[:5]) == "%PDF-"
}

// randomHex generates a random hex string of n bytes.
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
