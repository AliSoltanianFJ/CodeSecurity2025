package main

import (
    "bufio"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
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
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit total request body size to 10MB
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20) // 10 MiB

    // Parse multipart form (memory limit; file will still be limited by MaxBytesReader)
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename sanitation and extension check
    origName := filepath.Base(header.Filename)
    if origName == "" {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(origName))
    if ext != ".pdf" {
        http.Error(w, "File must have .pdf extension", http.StatusBadRequest)
        return
    }
    safeName := sanitizeFilename(origName)
    if safeName == "" {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Per-file size limit: 5MB
    const maxFileSize = 5 << 20 // 5 MiB
    limited := io.LimitReader(file, maxFileSize+1)

    // Use a buffered reader so we can peek header bytes without consuming them
    br := bufio.NewReader(limited)
    peek, err := br.Peek(4) // PDF files start with "%PDF"
    if err != nil {
        http.Error(w, "Unable to read file", http.StatusBadRequest)
        return
    }
    if string(peek) != "%PDF" {
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with restrictive permissions
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o700); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Generate a random filename prefix to avoid collisions and discourage guessing
    rb := make([]byte, 16)
    if _, err := rand.Read(rb); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    prefix := hex.EncodeToString(rb)
    dstName := prefix + "_" + safeName
    dstPath := filepath.Join(uploadDir, dstName)

    // Create destination file atomically and with restrictive permissions
    out, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0o600)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        out.Close()
    }()

    // Copy the file, respecting the size limit enforced by the LimitReader.
    written, err := io.Copy(out, br)
    if err != nil {
        // remove partial file on error
        out.Close()
        _ = os.Remove(dstPath)
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written > maxFileSize {
        out.Close()
        _ = os.Remove(dstPath)
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Success: do not leak internal paths or sensitive info
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("uploaded"))
}

// sanitizeFilename returns a safe filename containing only a small set of allowed characters.
// It strips directory components, collapses disallowed characters to '_', and prevents names that start with dot.
func sanitizeFilename(name string) string {
    name = filepath.Base(name)
    if name == "" {
        return ""
    }
    var b strings.Builder
    for _, r := range name {
        switch {
        case r >= 'a' && r <= 'z':
            b.WriteRune(r)
        case r >= 'A' && r <= 'Z':
            b.WriteRune(r)
        case r >= '0' && r <= '9':
            b.WriteRune(r)
        case r == '.' || r == '_' || r == '-':
            b.WriteRune(r)
        // anything else -> underscore
        default:
            b.WriteRune('_')
        }
    }
    out := b.String()
    // prevent hidden files like ".pdf"
    if strings.HasPrefix(out, ".") {
        out = "uploaded" + out
    }
    // collapse repeated underscores (optional)
    out = strings.ReplaceAll(out, "__", "_")
    return out
}
// ...existing code...
