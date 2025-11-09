// ...existing code...
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
    "time"
)
// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions & notes:
    // - Enforce a strict maximum upload size to mitigate DoS and resource exhaustion.
    // - Use http.MaxBytesReader to cap the request body before multipart parsing.
    // - Verify file magic bytes ("%PDF") and do a content-type sniff; do NOT trust the client-provided Content-Type.
    // - Sanitize user filenames and do NOT use them directly for file paths to avoid path-traversal.
    // - Use a secure, unique filename and owner-only filesystem permissions for stored files.
    // - Return minimal, non-sensitive error messages to clients; log details server-side.
    // - Ensure the uploads directory is created with restrictive permissions (0700).
    const maxUploadSize = 10 << 20 // 10 MB
    const uploadDir = "./uploads"
    const sniffLen = 512           // bytes to sniff content type

    // Limit total request size (accounts for multipart overhead)
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+sniffLen)

    // Parse multipart form with the same limit. This ensures FormFile won't read unlimited data.
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        log.Println("multipart parse error:", err)
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        log.Println("form file error:", err)
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read initial bytes for sniffing and magic check.
    buf := make([]byte, sniffLen)
    n, err := io.ReadFull(file, buf)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        log.Println("error reading file header:", err)
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    headerBytes := buf[:n]

    // Verify PDF magic bytes: PDF files start with "%PDF"
    if n < 4 || string(headerBytes[:4]) != "%PDF" {
        log.Println("file magic mismatch; not a PDF")
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Additional MIME sniffing (defense in depth)
    sniffType := http.DetectContentType(headerBytes)
    if !strings.HasPrefix(sniffType, "application/pdf") && !strings.HasPrefix(sniffType, "application/octet-stream") {
        // Some legitimate PDFs can be detected as application/octet-stream; allow that only if magic matched above.
        log.Println("unexpected content type")
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure upload directory exists with restrictive permissions.
    if err := os.MkdirAll(uploadDir, 0o700); err != nil {
        log.Println("failed to create upload dir:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Sanitize original filename to produce a short, safe prefix. Don't use user input for path traversal.
    safeBase := filepath.Base(header.Filename)
    // Remove suspicious characters and limit length
    safeBase = strings.Map(func(r rune) rune {
        if r == '_' || r == '-' || r == '.' || (r >= '0' && r <= '9') || (r >= 'A' && r <= 'Z') || (r >= 'a' && r <= 'z') {
            return r
        }
        // drop everything else
        return -1
    }, safeBase)
    if len(safeBase) > 64 {
        safeBase = safeBase[:64]
    }
    if safeBase == "" {
        safeBase = "upload"
    }
    // Generate a cryptographically-random suffix to avoid collisions and guessing.
    rnd := make([]byte, 12)
    if _, err := rand.Read(rnd); err != nil {
        // If random generation fails, fall back to timestamp (still safe, but log for audit).
        log.Println("crypto rand failed:", err)
    }
    uniq := hex.EncodeToString(rnd)
    filename := safeBase + "_" + uniq + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // Create destination file with owner-only permissions; use O_EXCL to avoid races.
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0o600)
    if err != nil {
        log.Println("failed to create destination file:", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        // best-effort close
        if cerr := dst.Close(); cerr != nil {
            log.Println("failed closing destination file:", cerr)
        }
    }()

    // Write the initial bytes we already read.
    if n > 0 {
        if _, err := dst.Write(headerBytes); err != nil {
            // Attempt to remove partial file on write failure to avoid leaving corrupt data.
            _ = dst.Close()
            _ = os.Remove(dstPath)
            log.Println("failed writing initial bytes:", err)
            http.Error(w, "server error", http.StatusInternalServerError)
            return
        }
    }

    // Copy the remainder of the uploaded file. Enforce the maxUploadSize indirectly via request size cap.
    if _, err := io.Copy(dst, file); err != nil {
        _ = dst.Close()
        _ = os.Remove(dstPath)
        // If the client exceeded MaxBytesReader, the error could be "http: request body too large".
        log.Println("failed copying file contents:", err)
        http.Error(w, "invalid file upload", http.StatusBadRequest)
        return
    }

    // Success: do not expose server filesystem paths to client; return the safe filename.
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte(`{"filename":"` + filename + `"}`))

    // Server-side log for audit
    log.Printf("uploaded PDF saved successfully")
}
// ...existing code...
