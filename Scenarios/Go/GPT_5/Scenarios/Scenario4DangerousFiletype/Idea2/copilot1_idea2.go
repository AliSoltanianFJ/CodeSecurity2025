package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "encoding/json"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    maxUploadSize = 10 << 20 // 10 MB
    uploadsDir    = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)

    // Do not reveal server internals in errors; log internal errors only.
    log.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server exited: %v", err)
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to prevent large uploads / DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse multipart form (will respect MaxBytesReader limit)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid multipart form or file too large", http.StatusBadRequest)
        return
    }

    // Retrieve file
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing 'pdf' form file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename validation: strip any path components
    origName := filepath.Base(header.Filename)
    if origName == "." || origName == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Ensure extension is .pdf (case-insensitive)
    if !strings.HasSuffix(strings.ToLower(origName), ".pdf") {
        http.Error(w, "file must have .pdf extension", http.StatusBadRequest)
        return
    }

    // Read a small header to validate PDF magic bytes and prevent spoofing
    var headerBuf [512]byte
    n, err := file.Read(headerBuf[:])
    if err != nil && err != io.EOF {
        log.Printf("error reading file header: %v", err)
        http.Error(w, "unable to read uploaded file", http.StatusInternalServerError)
        return
    }
    if n < 4 || !bytes.HasPrefix(headerBuf[:n], []byte("%PDF")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with restrictive permissions
    if err := os.MkdirAll(uploadsDir, 0700); err != nil {
        log.Printf("unable to create uploads dir: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Generate unpredictable filename to avoid directory traversal and collisions
    id := make([]byte, 16)
    if _, err := rand.Read(id); err != nil {
        log.Printf("unable to generate filename: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    safeName := hex.EncodeToString(id) + ".pdf"
    destPath := filepath.Join(uploadsDir, safeName)

    // Create destination with restrictive permissions
    dst, err := os.OpenFile(destPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0600)
    if err != nil {
        log.Printf("unable to open destination file: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the bytes already read, then copy the rest while still respecting MaxBytesReader
    if n > 0 {
        if _, err := dst.Write(headerBuf[:n]); err != nil {
            log.Printf("error writing file (initial chunk): %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }
    }
    if _, err := io.Copy(dst, file); err != nil {
        // If copy fails due to size limits, treat as bad request
        // but do not leak internal details to client.
        log.Printf("error copying uploaded file: %v", err)
        http.Error(w, "failed to save uploaded file", http.StatusInternalServerError)
        return
    }

    // Respond with JSON containing the stored filename only (no sensitive info)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent content sniffing by browsers
    w.Header().Set("X-Content-Type-Options", "nosniff")

    resp := struct {
        Filename string `json:"filename"`
    }{
        Filename: safeName,
    }
    // Do not include original filename or any private info in response
    if err := json.NewEncoder(w).Encode(resp); err != nil {
        log.Printf("error encoding response: %v", err)
    }
}