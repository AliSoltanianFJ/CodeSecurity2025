package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "encoding/json"
    "fmt"
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

// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce max upload size for the request body
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse the multipart form (memory threshold equals max size to avoid surprises).
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "failed to parse multipart form: "+http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file field", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read the first bytes to validate PDF signature and content type
    const sniffLen = 512
    prefix := make([]byte, sniffLen)
    n, err := file.Read(prefix)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file", http.StatusInternalServerError)
        return
    }
    prefix = prefix[:n]

    // Simple and reliable PDF signature check: files should start with "%PDF-"
    if !bytes.HasPrefix(prefix, []byte("%PDF-")) {
        // Also check detected content type as a fallback
        if ct := http.DetectContentType(prefix); !strings.HasPrefix(ct, "application/pdf") {
            http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
            return
        }
    }

    // Prepare uploads directory with restricted permissions
    uploadDir := "uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // Generate a safe, unique filename (do not use user-supplied filename directly)
    randomBytes := make([]byte, 16)
    if _, err := rand.Read(randomBytes); err != nil {
        http.Error(w, "failed to generate filename", http.StatusInternalServerError)
        return
    }
    uniqueName := hex.EncodeToString(randomBytes) + ".pdf"
    dstPath := filepath.Join(uploadDir, uniqueName)

    // Ensure we don't accidentally overwrite an existing file
    dstFile, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dstFile.Close()

    // Write the prefix we already read, then stream the rest. Limit copy to maxUploadSize.
    reader := io.MultiReader(bytes.NewReader(prefix), file)
    if _, err := io.Copy(dstFile, io.LimitReader(reader, maxUploadSize)); err != nil {
        // On write failure, try to remove incomplete file
        _ = dstFile.Close()
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save uploaded file", http.StatusInternalServerError)
        return
    }

    // Respond with JSON containing only the server-side filename (no user data echoed back)
    w.Header().Set("Content-Type", "application/json")
    resp := map[string]string{
        "file": uniqueName,
        "size": fmt.Sprintf("%d", header.Size),
    }
    // Use encoding/json to safely serialize; this prevents XSS via manual string building
    _ = json.NewEncoder(w).Encode(resp)
}
