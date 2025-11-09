// ...existing code...
package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const maxUploadSize = int64(10 << 20) // 10 MB

    // limit request body size to avoid resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512)

    // Parse the multipart form with memory limit
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large or malformed", http.StatusRequestEntityTooLarge)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read up to 512 bytes to validate PDF magic bytes
    head := make([]byte, 512)
    n, err := file.Read(head)
    if err != nil && err != io.EOF {
        log.Printf("read header error: %v", err)
        http.Error(w, "unable to read file", http.StatusBadRequest)
        return
    }
    if n < 5 || !bytes.HasPrefix(head[:n], []byte("%PDF-")) {
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }

    // Ensure upload directory exists with restrictive permissions
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        log.Printf("mkdir error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Generate a safe, unpredictable filename (do not use user-supplied filename)
    rnd := make([]byte, 12)
    if _, err := rand.Read(rnd); err != nil {
        log.Printf("rand error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    filename := fmt.Sprintf("%d_%s.pdf", time.Now().UnixNano(), hex.EncodeToString(rnd))
    dstPath := filepath.Join(uploadDir, filename)

    // Create file with exclusive create to avoid overwriting
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        log.Printf("open destination error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        if err := dst.Close(); err != nil {
            log.Printf("close dst error: %v", err)
        }
    }()

    // Combine the bytes already read with the remaining stream
    reader := io.MultiReader(bytes.NewReader(head[:n]), file)

    // Copy with a limit of maxUploadSize + 1 so we can detect oversized files
    written, err := io.Copy(dst, io.LimitReader(reader, maxUploadSize+1))
    if err != nil && err != io.EOF {
        // If an error occurred during write, remove partial file and report generic error
        _ = os.Remove(dstPath)
        log.Printf("write error: %v", err)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }
    if written > maxUploadSize {
        // remove oversize file and respond with appropriate status
        _ = os.Remove(dstPath)
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Successful upload: respond without exposing internal paths or filenames
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("file uploaded"))
}
// ...existing code...
