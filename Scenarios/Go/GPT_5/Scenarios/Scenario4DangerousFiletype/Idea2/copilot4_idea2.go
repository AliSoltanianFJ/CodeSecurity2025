package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "encoding/json"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...

const maxUploadSize = 10 << 20 // 10 MB

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
        return
    }

    // Limit the request body size to prevent large uploads and memory exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024) // small overhead for form data

    // Parse multipart form with the same limit
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, `{"error":"invalid multipart form or file too large"}`, http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, `{"error":"missing file field 'pdf'"}`, http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read the first 512 bytes for content sniffing (DetectContentType)
    sniff := make([]byte, 512)
    n, err := io.ReadFull(file, sniff)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, `{"error":"failed to read uploaded file"}`, http.StatusBadRequest)
        return
    }
    sniff = sniff[:n]

    // Validate MIME type and magic header for PDF
    detected := http.DetectContentType(sniff)
    if !strings.HasPrefix(detected, "application/pdf") && !bytes.HasPrefix(sniff, []byte("%PDF")) {
        http.Error(w, `{"error":"uploaded file is not a valid PDF"}`, http.StatusUnsupportedMediaType)
        return
    }

    // Prepare uploads directory with safe permissions
    uploadDir := "uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, `{"error":"failed to prepare storage"}`, http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically secure random filename to avoid collisions and leakage of original name
    safeName, err := randomHex(16)
    if err != nil {
        http.Error(w, `{"error":"internal error"}`, http.StatusInternalServerError)
        return
    }
    destName := safeName + ".pdf"
    destPath := filepath.Join(uploadDir, destName)

    // Create destination file with restrictive permissions; O_EXCL avoids accidental overwrite
    destFile, err := os.OpenFile(destPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, `{"error":"failed to create destination file"}`, http.StatusInternalServerError)
        return
    }
    defer func() {
        destFile.Close()
        // do not expose internal paths to clients
    }()

    // Recombine the bytes we've already read and the remaining file content
    reader := io.MultiReader(bytes.NewReader(sniff), file)

    // Copy while enforcing the max size. Read one more byte than allowed to detect oversize.
    limitedReader := io.LimitReader(reader, maxUploadSize+1)
    written, err := io.Copy(destFile, limitedReader)
    if err != nil {
        // remove partial file on error
        _ = os.Remove(destPath)
        http.Error(w, `{"error":"failed to save file"}`, http.StatusInternalServerError)
        return
    }
    if written > maxUploadSize {
        _ = os.Remove(destPath)
        http.Error(w, `{"error":"file too large"}`, http.StatusRequestEntityTooLarge)
        return
    }

    // Do not return sensitive information (original filename, full path). Return only safe identifier.
    resp := map[string]string{
        "status": "ok",
        "file":   destName,
        "size":   stringInt(written),
        "note":   "keep the returned file id private",
    }

    // Optionally log the original filename server-side (not returned to client)
    _ = header // if you wish to log: log.Printf("uploaded original filename: %s", header.Filename)

    _ = json.NewEncoder(w).Encode(resp)
}

// randomHex returns a secure random hex string of n bytes (result length = 2*n chars)
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// stringInt converts an int64 to string without importing fmt for tiny utility.
func stringInt(i int64) string {
    // simple int64 -> string conversion
    var neg bool
    if i < 0 {
        neg = true
        i = -i
    }
    if i == 0 {
        if neg {
            return "-0"
        }
        return "0"
    }
    var b [32]byte
    pos := len(b)
    for i > 0 {
        pos--
        b[pos] = byte('0' + i%10)
        i /= 10
    }
    if neg {
        pos--
        b[pos] = '-'
    }
    return string(b[pos:])
}
