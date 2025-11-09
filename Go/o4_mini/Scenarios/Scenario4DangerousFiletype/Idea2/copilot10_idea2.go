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
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce max size for the request body
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid multipart form or file too large", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file field 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename sanity check (do not use user filename for storage)
    origName := filepath.Base(header.Filename)
    if origName == "." || origName == "/" || origName == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(origName))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Read initial bytes to validate PDF magic header and avoid relying solely on client MIME
    head := make([]byte, 512)
    n, err := io.ReadFull(file, head)
    if err != nil && err != io.ErrUnexpectedEOF {
        http.Error(w, "cannot read file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(head[:n], []byte("%PDF-")) {
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Reconstruct reader to include the bytes we've already consumed
    reader := io.MultiReader(bytes.NewReader(head[:n]), file)

    // Prepare uploads directory
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o750); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically secure filename to avoid collisions and path traversal
    randomBytes := make([]byte, 16)
    if _, err := rand.Read(randomBytes); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    safeName := hex.EncodeToString(randomBytes) + ".pdf"
    dstPath := filepath.Join(uploadDir, safeName)

    // Create file with restrictive permissions; fail if it already exists (shouldn't)
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o640)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy with a limit to ensure we never write more than allowed
    if _, err := io.Copy(dst, io.LimitReader(reader, maxUploadSize)); err != nil {
        // Remove partial file on error
        _ = dst.Close()
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // Minimal, non-sensitive response
    w.WriteHeader(http.StatusCreated)
    _, _ = fmt.Fprint(w, "uploaded")
}
// ...existing code...
