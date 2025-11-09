package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    // Maximum accepted upload size: 10 MiB. Adjust as appropriate for your service.
    maxUploadSize = 10 << 20 // 10 MB

    // Directory to store uploads. Kept relative here for example; in production use an absolute path
    // that is isolated and owned by the service account with least privileges.
    uploadsDir = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)

    // NOTE: For transport security, run behind TLS (HTTPS). Do not expose plain HTTP in production.
    // e.g. use Reverse Proxy (nginx, Caddy) terminating TLS, or call ListenAndServeTLS with proper certs.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce POST-only to avoid unexpected behavior.
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to mitigate DoS and large file uploads.
    // We add a small margin for multipart overhead.
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512)

    // Parse the multipart form for the request body. This does not store files on disk
    // because we stream the file below. Using ParseMultipartForm with a small memory threshold.
    if err := r.ParseMultipartForm(1024); err != nil {
        log.Printf("error parsing multipart form from %s: %v", r.RemoteAddr, err)
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        log.Printf("missing 'pdf' form file from %s: %v", r.RemoteAddr, err)
        http.Error(w, "file is required", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read the first bytes to validate file magic (PDF begins with "%PDF-").
    // Use a small buffer to avoid reading large amounts into memory.
    const sniffLen = 512
    sniff := make([]byte, sniffLen)
    n, readErr := io.ReadFull(file, sniff)
    if readErr != nil && readErr != io.ErrUnexpectedEOF && readErr != io.EOF {
        log.Printf("error reading uploaded file header from %s: %v", r.RemoteAddr, readErr)
        http.Error(w, "unable to read file", http.StatusBadRequest)
        return
    }
    sniff = sniff[:n]

    // Verify PDF signature (very small check) to defend against dangerous filetypes.
    if !isPDF(sniff) {
        log.Printf("rejected upload (not PDF) from %s, filename=%q", r.RemoteAddr, sanitizeFilename(header.Filename))
        http.Error(w, "invalid file type", http.StatusBadRequest)
        return
    }

    // Generate a secure random filename and do not use user-supplied filename directly to avoid path traversal.
    randomName, err := randomHex(16)
    if err != nil {
        log.Printf("error generating filename for upload from %s: %v", r.RemoteAddr, err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    stagingName := randomName + ".tmp"
    finalName := randomName + ".pdf"

    // Ensure upload directory exists with restrictive permissions.
    if err := os.MkdirAll(uploadsDir, 0700); err != nil {
        log.Printf("failed to create upload dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    stagingPath := filepath.Join(uploadsDir, stagingName)
    finalPath := filepath.Join(uploadsDir, finalName)

    // Create temporary file in the uploads directory with restrictive permissions.
    out, err := os.OpenFile(stagingPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0600)
    if err != nil {
        log.Printf("failed to create staging file for %s: %v", r.RemoteAddr, err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Ensure we clean up staging file on any early return.
    cleanup := func() {
        out.Close()
        _ = os.Remove(stagingPath)
    }
    // Write the bytes we already read, then stream the remainder.
    if _, err := out.Write(sniff); err != nil {
        log.Printf("write error for upload from %s: %v", r.RemoteAddr, err)
        cleanup()
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Copy the rest of the file stream to disk. We use io.Copy which streams without loading entire file in memory.
    if _, err := io.Copy(out, file); err != nil {
        log.Printf("stream copy error for upload from %s: %v", r.RemoteAddr, err)
        cleanup()
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    if err := out.Close(); err != nil {
        log.Printf("error closing file for upload from %s: %v", r.RemoteAddr, err)
        cleanup()
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Finalize by atomically renaming staging file to final filename.
    // This reduces the window where a partially written file is visible.
    if err := os.Rename(stagingPath, finalPath); err != nil {
        log.Printf("failed to finalize upload for %s: %v", r.RemoteAddr, err)
        _ = os.Remove(stagingPath)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Log minimal, non-sensitive metadata for audit.
    log.Printf("uploaded PDF from %s as %s (original filename=%q, size=%d bytes)",
        r.RemoteAddr, finalName, sanitizeFilename(header.Filename), fileSize(finalPath))

    // Respond with minimal information. Do not leak server paths or internal errors.
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("uploaded"))
}

// isPDF checks for a PDF file header. This is a minimal sanity check and not a full parser.
func isPDF(b []byte) bool {
    return bytes.HasPrefix(b, []byte("%PDF-"))
}

// randomHex returns a securely generated random hex string of length 2*n.
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// sanitizeFilename returns a conservative sanitized version of the supplied filename.
// We only keep a small set of safe characters and truncate to a reasonable length.
func sanitizeFilename(name string) string {
    name = filepath.Base(name)                  // drop any path components
    name = strings.TrimSpace(name)
    if name == "" {
        return ""
    }
    // keep alphanum, dot, dash, underscore; replace others with underscore
    var out []rune
    for _, r := range name {
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') || r == '.' || r == '-' || r == '_' {
            out = append(out, r)
        } else {
            out = append(out, '_')
        }
        if len(out) >= 64 {
            break
        }
    }
    return string(out)
}

// fileSize returns the size of the file at path; returns 0 on error.
func fileSize(path string) int64 {
    info, err := os.Stat(path)
    if err != nil {
        return 0
    }
    return info.Size()
}
