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

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions documented in comments:
    // - Reject non-TLS requests to protect file contents in transit.
    // - Enforce a strict maximum upload size to avoid DoS and resource exhaustion.
    // - Validate file extension and magic bytes to reduce risk of dangerous file types.
    // - Sanitize the supplied filename and generate a unique filename to avoid collisions and path traversal.
    // - Store files in a dedicated uploads directory with least-privilege permissions.
    // - Do not expose internal errors to the client (log details server-side, return safe messages).
    const maxUploadSize = 10 << 20 // 10 MiB
    const uploadFormField = "pdf"
    const uploadDir = "./uploads"

    // Require TLS to protect sensitive data in transit.
    if r.TLS == nil {
        // Do not give implementation details to client.
        http.Error(w, "TLS required for file uploads", http.StatusUpgradeRequired)
        return
    }

    // Limit request body size to prevent large uploads
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024) // small headroom for form data

    // Parse the multipart form (memory usage limited by maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        log.Printf("upload: parse multipart form error: %v", err)
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile(uploadFormField)
    if err != nil {
        log.Printf("upload: retrieving form file error: %v", err)
        http.Error(w, "file required", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Sanitize filename and enforce .pdf extension
    origFilename := filepath.Base(header.Filename) // removes any path components
    if origFilename == "." || origFilename == "" {
        log.Printf("upload: empty filename")
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(origFilename))
    if ext != ".pdf" {
        log.Printf("upload: rejected extension %q for file %q", ext, origFilename)
        http.Error(w, "only PDF files are allowed", http.StatusBadRequest)
        return
    }
    nameOnly := strings.TrimSuffix(origFilename, ext)
    // Basic sanitization: allow a limited charset (letters, numbers, '-', '_')
    // Replace other characters with underscore to avoid problematic names.
    var sb strings.Builder
    for _, r := range nameOnly {
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '-' || r == '_' {
            sb.WriteRune(r)
        } else {
            sb.WriteByte('_')
        }
    }
    safeBase := sb.String()
    if safeBase == "" {
        safeBase = "upload"
    }

    // Read the first bytes to validate PDF magic header: "%PDF-"
    peek := make([]byte, 512)
    n, err := file.Read(peek)
    if err != nil && err != io.EOF {
        log.Printf("upload: reading file header error: %v", err)
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if n < 4 || !bytes.HasPrefix(peek[:n], []byte("%PDF")) {
        log.Printf("upload: file %q rejected: magic header mismatch", origFilename)
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Reconstruct reader to include the bytes we consumed
    reader := io.MultiReader(bytes.NewReader(peek[:n]), file)

    // Ensure upload directory exists with restrictive permissions (0700)
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        log.Printf("upload: cannot create upload dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically random suffix to avoid collisions and guessing
    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        log.Printf("upload: random generation failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    suffix := hex.EncodeToString(randBytes)
    dstName := fmt.Sprintf("%s_%s.pdf", safeBase, suffix)
    dstPath := filepath.Join(uploadDir, dstName)

    // Open destination file with O_EXCL to avoid race conditions and set restrictive permissions (0600)
    dstFile, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        log.Printf("upload: cannot create destination file: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        // Ensure file is closed; if an error occurred during write, attempt to remove the partial file
        if cerr := dstFile.Close(); cerr != nil {
            log.Printf("upload: error closing file %q: %v", dstPath, cerr)
        }
    }()

    // Copy with explicit size limit to enforce maxUploadSize
    written, err := io.Copy(dstFile, io.LimitReader(reader, maxUploadSize))
    if err != nil {
        // Attempt to remove partial file to avoid leaving incomplete or corrupted files
        _ = os.Remove(dstPath)
        log.Printf("upload: write failed for %q: %v", dstPath, err)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }
    if written == maxUploadSize {
        // If we hit the limit exactly, the client may have sent a larger file than allowed.
        // Remove the file and reject the upload.
        _ = os.Remove(dstPath)
        log.Printf("upload: file %q exceeds maximum allowed size", origFilename)
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Success: do not leak internal path; return safe, minimal information.
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("upload successful"))
}
// ...existing code...
