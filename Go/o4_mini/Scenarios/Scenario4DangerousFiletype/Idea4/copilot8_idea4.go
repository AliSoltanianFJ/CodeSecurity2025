package main

import (
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
    // Security decisions (documented):
    // - Require POST and multipart/form-data.
    // - Enforce a maximum upload size to avoid DoS and resource exhaustion.
    // - Validate file extension and verify PDF magic bytes to prevent dangerous file types.
    // - Do not use the provided filename directly (prevent path traversal); generate a secure random filename.
    // - Store uploads under ./uploads with restrictive permissions and use O_EXCL to avoid races.
    // - Return only minimal information to the client; log detailed errors server-side only.
    // - Recommend running the server behind HTTPS (this handler checks r.TLS optionally) and using CSRF protections in production.

    const maxUploadSize = 10 << 20 // 10 MB

    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic Content-Type check
    ct := r.Header.Get("Content-Type")
    if !strings.Contains(strings.ToLower(ct), "multipart/form-data") {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }

    // Recommend TLS in production: refuse insecure requests if running with TLS expectation.
    // If you run this behind a TLS-terminating proxy, remove this check and ensure the proxy enforces TLS.
    if r.TLS == nil {
        // Do not reveal details; just hint that secure transport is required.
        http.Error(w, "transport must be secure (HTTPS)", http.StatusUpgradeRequired)
        return
    }

    // Prevent large uploads: wrap Body with MaxBytesReader
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)

    // Parse the multipart form in a memory-efficient way by using FormFile after MaxBytesReader.
    file, header, err := r.FormFile("pdf")
    if err != nil {
        // Log server-side, but return a generic error to the client.
        log.Printf("upload: FormFile error: %v", err)
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate declared extension is .pdf (case-insensitive).
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Read the first bytes to verify PDF magic header. Do not assume extension is sufficient.
    const magicLen = 5
    buf := make([]byte, 512) // read up to 512 bytes for header sniffing
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        log.Printf("upload: reading header failed: %v", err)
        http.Error(w, "failed to read file", http.StatusInternalServerError)
        return
    }
    if n < magicLen || string(buf[:magicLen]) != "%PDF-" {
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Prepare uploads directory with restrictive permissions (owner only).
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        log.Printf("upload: creating uploads dir failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a secure random filename instead of trusting user input.
    nonce := make([]byte, 16)
    if _, err := rand.Read(nonce); err != nil {
        log.Printf("upload: nonce generation failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(nonce) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // Create the destination file with exclusive create and restrictive permissions.
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        log.Printf("upload: creating destination file failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Ensure we always close the destination file.
    defer func() {
        if cerr := dst.Close(); cerr != nil {
            log.Printf("upload: closing destination failed: %v", cerr)
        }
    }()

    // Write the bytes we already read, then copy the remainder. Respect the max upload size enforced above.
    if n > 0 {
        if _, err := dst.Write(buf[:n]); err != nil {
            log.Printf("upload: writing header to destination failed: %v", err)
            http.Error(w, "server error", http.StatusInternalServerError)
            // Attempt to remove incomplete file (best-effort).
            _ = os.Remove(dstPath)
            return
        }
    }

    // Copy remaining file content
    if _, err := io.Copy(dst, file); err != nil {
        log.Printf("upload: writing file to destination failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        _ = os.Remove(dstPath)
        return
    }

    // Attempt to flush to disk.
    if err := dst.Sync(); err != nil {
        // Sync failure is logged but not disclosed to the user.
        log.Printf("upload: sync failed for %s: %v", dstPath, err)
    }

    // Success - return the generated filename only (no filesystem paths).
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    // Minimal response; client receives an opaque identifier for the uploaded file.
    fmt.Fprintf(w, `{"status":"ok","file":"%s"}`, filename)
}
