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
    "strings"
)

const maxUploadSize = 10 << 20 // 10 MB

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request size to prevent large uploads and DoS.
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse multipart form (stores small parts in memory)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid multipart form or too large", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate filename extension (do not trust client input alone)
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "file must have .pdf extension", http.StatusBadRequest)
        return
    }

    // Read up to 512 bytes to validate content (magic bytes and MIME sniffing)
    const sniffLen = 512
    head := make([]byte, sniffLen)
    n, err := io.ReadFull(file, head)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, "error reading file", http.StatusInternalServerError)
        return
    }
    head = head[:n]

    // Check PDF magic header "%PDF"
    if !bytes.HasPrefix(head, []byte("%PDF")) {
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Double-check MIME type heuristically
    detected := http.DetectContentType(head)
    if !strings.Contains(strings.ToLower(detected), "pdf") {
        // Not strictly necessary if magic bytes passed, but keep defense-in-depth.
        http.Error(w, "file content is not recognized as PDF", http.StatusBadRequest)
        return
    }

    // Read the rest of the file (safe because we limited the request size above)
    rest, err := io.ReadAll(file)
    if err != nil {
        http.Error(w, "error reading file", http.StatusInternalServerError)
        return
    }

    data := append(head, rest...)
    if int64(len(data)) > maxUploadSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Ensure uploads directory exists with restrictive permissions
    uploadDir := filepath.Join(".", "uploads")
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically random filename to avoid path traversal
    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "unable to generate filename", http.StatusInternalServerError)
        return
    }
    safeName := hex.EncodeToString(randBytes) + ".pdf"
    destPath := filepath.Join(uploadDir, safeName)

    // Write file with restrictive permissions
    if err := os.WriteFile(destPath, data, 0600); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // Do not echo back user-supplied filenames or file paths (prevents information disclosure).
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "upload successful")
}
