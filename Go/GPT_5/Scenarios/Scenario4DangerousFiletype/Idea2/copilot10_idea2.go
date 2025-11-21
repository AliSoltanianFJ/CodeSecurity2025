// ...existing code...
package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "encoding/json"
    "io"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const maxUploadSize = 10 << 20 // 10 MB

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require multipart/form-data
    ct := r.Header.Get("Content-Type")
    mediaType, _, err := mime.ParseMediaType(ct)
    if err != nil || mediaType != "multipart/form-data" {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }

    // Prevent large requests and multipart parsing memory spikes
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024) // small headroom for multipart overhead
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large or malformed multipart form", http.StatusRequestEntityTooLarge)
        return
    }

    // Obtain file
    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file field", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read first bytes for content sniffing (magic bytes + mime type)
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file", http.StatusInternalServerError)
        return
    }
    if n == 0 {
        http.Error(w, "empty file", http.StatusBadRequest)
        return
    }

    // Check magic PDF signature and MIME sniff
    if !bytes.HasPrefix(buf[:n], []byte("%PDF-")) && !strings.HasPrefix(http.DetectContentType(buf[:n]), "application/pdf") {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with safe permissions
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Generate cryptographically random filename (do NOT use user-supplied filename directly)
    fn, err := randomHex(16)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    fn = fn + ".pdf"
    dstPath := filepath.Join(uploadDir, fn)

    // Open destination file safely: O_EXCL to avoid overwriting
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0o644)
    if err != nil {
        http.Error(w, "failed to create file", http.StatusInternalServerError)
        return
    }

    // Combine the already-read bytes with the remaining file and limit total bytes written
    limitedReader := io.LimitReader(io.MultiReader(bytes.NewReader(buf[:n]), file), maxUploadSize)
    written, err := io.Copy(dst, limitedReader)
    if err != nil {
        dst.Close()
        // remove partial file
        _ = os.Remove(dstPath)
        http.Error(w, "failed while saving file", http.StatusInternalServerError)
        return
    }
    // Close destination file
    if err := dst.Close(); err != nil {
        _ = os.Remove(dstPath)
        http.Error(w, "failed to finalize file", http.StatusInternalServerError)
        return
    }

    // Respond with JSON containing only safe information
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    type resp struct {
        File  string `json:"file"`  // stored filename (safe, random)
        Bytes int64  `json:"bytes"` // size of saved file
    }
    _ = json.NewEncoder(w).Encode(resp{File: fn, Bytes: written})
}

func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
// ...existing code...
