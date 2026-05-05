package main

import (
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)

    // NOTE: In production, serve only over TLS (HTTPS). Use ListenAndServeTLS or place
    // the service behind a TLS-terminating reverse proxy. Leaving HTTP here for local testing.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions and rationale (documented):
    // - Only allow POST uploads.
    // - Limit request body size (MaxBytesReader) to defend against DoS and huge uploads.
    // - Validate Content-Type and file magic bytes (PDF signature "%PDF-") to avoid
    //   dangerous file types being uploaded with spoofed extensions.
    // - Do not trust client filename; generate a cryptographically random name.
    // - Store files in an uploads directory with restrictive permissions.
    // - Stream copy the file to disk; avoid loading the whole file into memory.
    // - Return generic errors to the client; log detailed errors server-side.
    // - Use least privilege for file perms (0600 for files, 0700 for directories).

    // Enforce POST method
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit upload size to 10 MiB (adjust as appropriate). Add a small extra for form overhead.
    const maxFileSize = 10 << 20 // 10 MiB
    r.Body = http.MaxBytesReader(w, r.Body, maxFileSize+1024)

    // Require a multipart form
    if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "multipart/form-data") {
        http.Error(w, "Invalid content type", http.StatusBadRequest)
        return
    }

    // Parse form (memory limit small since body already limited)
    if err := r.ParseMultipartForm(1 << 20); err != nil {
        log.Printf("multipart parse error: %v", err)
        http.Error(w, "Invalid multipart data", http.StatusBadRequest)
        return
    }

    // Get uploaded file
    file, header, err := r.FormFile("pdf")
    if err != nil {
        log.Printf("form file error: %v", err)
        http.Error(w, "Missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Defensive check: ensure request context hasn't been cancelled
    if err := r.Context().Err(); err != nil {
        log.Printf("request context error: %v", err)
        http.Error(w, "Request cancelled", http.StatusRequestTimeout)
        return
    }

    // Read the first 512 bytes to sniff content type and check magic bytes.
    // io.ReadFull would block if file shorter; use Read to get what's available up to 512.
    headerBuf := make([]byte, 512)
    n, err := file.Read(headerBuf)
    if err != nil && err != io.EOF {
        log.Printf("reading header bytes failed: %v", err)
        http.Error(w, "Failed to read uploaded file", http.StatusInternalServerError)
        return
    }
    if n < 5 {
        // file too small to be a valid PDF (minimum sign "%PDF-")
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Validate PDF magic bytes ("%PDF-")
    if string(headerBuf[:5]) != "%PDF-" {
        log.Printf("invalid pdf signature.")
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Content type sniffing for extra defense (DetectContentType examines up to 512 bytes).
    contentType := http.DetectContentType(headerBuf[:n])
    if !strings.HasPrefix(contentType, "application/pdf") {
        log.Printf("content type mismatch: detected=%q, filename=%q", contentType, header.Filename)
        http.Error(w, "Uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // Ensure upload directory exists with restrictive permissions.
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        log.Printf("failed to create upload directory: %v", err)
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically secure random filename. Do NOT use the client filename directly.
    randomBytes := make([]byte, 16)
    if _, err := rand.Read(randomBytes); err != nil {
        log.Printf("failed to generate random filename: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    name := hex.EncodeToString(randomBytes) + ".pdf"
    destPath := filepath.Join(uploadDir, name)

    // Create the destination file with exclusive create and restrictive permissions.
    out, err := os.OpenFile(destPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        log.Printf("failed to create destination file: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // If write fails, ensure file removed to avoid partial artifacts.
    cleanup := func() {
        out.Close()
        os.Remove(destPath)
    }
    // Write the header bytes already read, then stream the remainder up to the size limit.
    if _, err := out.Write(headerBuf[:n]); err != nil {
        log.Printf("failed to write initial bytes: %v", err)
        cleanup()
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // Copy remainder within the same limit. Using io.Copy will read until EOF; body is already limited by MaxBytesReader.
    if _, err := io.Copy(out, file); err != nil {
        log.Printf("error copying file contents: %v", err)
        cleanup()
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // Close output file and ensure data flushed
    if err := out.Close(); err != nil {
        // Attempt to remove partial file on failure
        os.Remove(destPath)
        log.Printf("error closing output file: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Successful upload: respond with minimal info. Do not expose filesystem paths in production.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}
