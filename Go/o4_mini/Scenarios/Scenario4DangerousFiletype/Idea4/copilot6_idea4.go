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
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // NOTE: In production, run behind TLS (ListenAndServeTLS or a reverse proxy terminating TLS).
    // Serving uploads or sensitive endpoints over plain HTTP is insecure.
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions (documented):
    // - Limit request size to prevent DoS and resource exhaustion.
    // - Validate HTTP method.
    // - Validate file magic bytes (PDF header) instead of trusting client MIME type.
    // - Do not use client-provided filename for storage; generate a safe, unique filename.
    // - Ensure upload directory exists with restrictive permissions.
    // - Use O_EXCL to prevent overwriting existing files.
    // - Return generic errors to clients; log internal details server-side.
    // - Recommend TLS in production (handled outside this handler).

    const maxUploadSize = 10 << 20 // 10 MiB
    const uploadField = "pdf"
    const uploadsDir = "./uploads"

    // Enforce POST
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit size of request body to mitigate large uploads
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse multipart form with same limit
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        log.Printf("upload: failed to parse multipart form: %v", err)
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile(uploadField)
    if err != nil {
        log.Printf("upload: missing form field %q: %v", uploadField, err)
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read a header sample to validate file magic bytes and detect content type.
    // Use 512 bytes as per net/http DetectContentType recommendation.
    const sniffLen = 512
    sniffBuf := make([]byte, sniffLen)
    n, err := io.ReadFull(file, sniffBuf)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        log.Printf("upload: error reading file header: %v", err)
        http.Error(w, "cannot read file", http.StatusBadRequest)
        return
    }
    sniffSample := sniffBuf[:n]

    // Check PDF magic: PDF files start with "%PDF-"
    if !bytes.HasPrefix(sniffSample, []byte("%PDF")) {
        // Client may send a file with wrong extension; reject.
        log.Printf("upload: rejected non-pdf upload from %s (filename=%q, detected=%q)", r.RemoteAddr, header.Filename, http.DetectContentType(sniffSample))
        http.Error(w, "invalid pdf file", http.StatusBadRequest)
        return
    }

    // Reconstruct a reader that yields the sniffed bytes first, then the remainder of the file.
    reader := io.MultiReader(bytes.NewReader(sniffSample), file)

    // Prepare uploads directory with restrictive permissions (owner only).
    if err := os.MkdirAll(uploadsDir, 0o700); err != nil {
        log.Printf("upload: cannot create uploads dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a secure random filename; do not trust client's filename.
    const randBytes = 12
    rb := make([]byte, randBytes)
    if _, err := rand.Read(rb); err != nil {
        log.Printf("upload: cannot generate filename: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    timestamp := time.Now().UTC().Format("20060102T150405Z")
    safeFilename := fmt.Sprintf("%s-%s.pdf", timestamp, hex.EncodeToString(rb))

    // Ensure the final path is within uploadsDir (defend against any oddities).
    destPath := filepath.Join(uploadsDir, safeFilename)
    absUploadsDir, err := filepath.Abs(uploadsDir)
    if err != nil {
        log.Printf("upload: cannot resolve uploads dir abs path: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absDestPath, err := filepath.Abs(destPath)
    if err != nil {
        log.Printf("upload: cannot resolve dest path abs: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if !strings.HasPrefix(absDestPath, absUploadsDir+string(os.PathSeparator)) && absDestPath != absUploadsDir {
        // Defensive: should never happen because we used Join with fixed uploadsDir.
        log.Printf("upload: destination path outside uploads dir: %s", absDestPath)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Create file atomically; do not overwrite existing files. Set mode to 0600 (owner read/write).
    out, err := os.OpenFile(destPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0o600)
    if err != nil {
        log.Printf("upload: cannot create file %s: %v", destPath, err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        // Ensure file is closed; if an error occurred during copy we remove the partial file.
        if cerr := out.Close(); cerr != nil {
            log.Printf("upload: error closing output file: %v", cerr)
        }
    }()

    // Copy the content with a sane io.Copy (will stop at maxUploadSize due to MaxBytesReader).
    written, err := io.Copy(out, reader)
    if err != nil {
        // Remove partial file on error to avoid leaving corrupted files.
        _ = os.Remove(destPath)
        log.Printf("upload: error writing to disk for %s: %v", destPath, err)
        http.Error(w, "cannot save file", http.StatusInternalServerError)
        return
    }

    // Log upload with limited, non-sensitive details only.
    log.Printf("upload: saved %s (orig=%q) size=%d bytes from %s", safeFilename, header.Filename, written, r.RemoteAddr)

    // Return success. Avoid exposing internal paths. Provide the generated safe filename.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte(safeFilename))
}
