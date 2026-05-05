package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...

    // Limit overall upload size to 10 MiB.
    const maxUploadSize = 10 << 20 // 10 MiB

    // Protect server from large requests
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse multipart form within the same limit
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid upload (too large or malformed)", http.StatusBadRequest)
        return
    }

    // Retrieve the file from posted form-data
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read first 512 bytes to sniff content-type and check PDF magic bytes.
    head := make([]byte, 512)
    n, _ := file.Read(head)
    head = head[:n]

    // Basic PDF magic check: PDF files start with "%PDF"
    if !bytes.HasPrefix(head, []byte("%PDF")) {
        // Additional MIME sniff for defence in depth
        detected := http.DetectContentType(head)
        if !strings.Contains(detected, "pdf") {
            http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
            return
        }
    }

    // Ensure uploads directory exists with safe permissions
    uploadDir := "uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        // Log internal error but don't return internal details to client
        log.Printf("failed creating upload dir: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically-random filename; do not trust user filename.
    filename := safeRandomFilename() + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // Create destination file with safe permissions
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o644)
    if err != nil {
        log.Printf("failed to create dst file: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Reconstruct a reader that includes the header bytes already read.
    reader := io.MultiReader(bytes.NewReader(head), file)

    // Copy with an additional limit to ensure we don't exceed maxUploadSize.
    written, err := io.Copy(dst, io.LimitReader(reader, maxUploadSize+1))
    if err != nil {
        // On error, attempt to remove partial file
        _ = os.Remove(dstPath)
        log.Printf("failed writing file: %v", err)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }
    if written > maxUploadSize {
        _ = os.Remove(dstPath)
        http.Error(w, "file too large", http.StatusBadRequest)
        return
    }

    // Don't reveal internal filenames or paths. Give a generic success response.
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("file uploaded successfully"))
}

// safeRandomFilename returns a collision-resistant random hex string.
// Falls back to time-based string if crypto randomness fails.
func safeRandomFilename() string {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err == nil {
        return hex.EncodeToString(b)
    }
    // fallback (very unlikely)
    return strings.ReplaceAll(time.Now().UTC().Format("20060102T150405.000000000Z"), ":", "-")
}
