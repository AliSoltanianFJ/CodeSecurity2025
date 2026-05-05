// ...existing code...
package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
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
    // Only allow POST
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const maxUploadSize = 10 << 20 // 10 MB
    // enforce max size on the request body
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // parse multipart form (memory limit)
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf form field", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename sanitization: drop directory components and disallow suspicious chars
    origName := filepath.Base(header.Filename)
    // allow only letters, numbers, dot, underscore and hyphen in a filename
    re := regexp.MustCompile(`[^a-zA-Z0-9._-]`)
    safeName := re.ReplaceAllString(origName, "_")

    // Ensure .pdf extension
    if strings.ToLower(filepath.Ext(safeName)) != ".pdf" {
        http.Error(w, "file must have .pdf extension", http.StatusBadRequest)
        return
    }

    // Read first bytes to validate PDF magic header and to avoid re-reading the stream.
    // Use a small buffer to avoid large allocations.
    magic := make([]byte, 4)
    n, err := io.ReadFull(file, magic)
    if err != nil || n < 4 {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(magic, []byte("%PDF")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Reconstruct reader to include the bytes we consumed
    reader := io.MultiReader(bytes.NewReader(magic), file)

    // Ensure uploads directory exists with restrictive permissions
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a safe, unpredictable filename (don't expose original path)
    rb := make([]byte, 8)
    if _, err := rand.Read(rb); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    randomHex := hex.EncodeToString(rb)
    newName := fmt.Sprintf("%d_%s.pdf", time.Now().Unix(), randomHex)
    dstPath := filepath.Join(uploadDir, newName)

    // Create file with restrictive permissions and fail if it already exists
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "unable to create file", http.StatusInternalServerError)
        return
    }
    defer func() {
        _ = dst.Close()
    }()

    // Copy with an explicit limit to protect from oversized uploads
    written, err := io.Copy(dst, io.LimitReader(reader, maxUploadSize+1))
    if err != nil {
        // remove partial file if something went wrong
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }
    if written > maxUploadSize {
        _ = os.Remove(dstPath)
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Do not reveal internal paths or original filenames. Return a minimal success message.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("upload successful"))
}
// ...existing code...
