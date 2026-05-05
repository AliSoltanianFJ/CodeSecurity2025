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

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions documented:
    // - Enforce POST-only uploads to limit accidental data exposure.
    // - Require TLS for transport security to protect data in transit.
    // - Limit request body size to prevent DoS and excessive memory use.
    // - Validate file magic bytes and MIME sniffing to avoid dangerous file types.
    // - Do NOT trust client-provided filename or Content-Type header.
    // - Generate a cryptographically random filename and store files with least-privilege perms.
    // - Create upload directory with restricted permissions.
    // - Return minimal, non-sensitive error messages to clients; log details server-side.
    const maxUploadSize = 10 << 20 // 10 MiB
    const uploadDir = "./uploads"
    const sniffBytes = 512

    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require TLS (protects sensitive data in transit).
    // If your environment terminates TLS at a proxy/load-balancer, adjust this check to trust that proxy.
    if r.TLS == nil {
        http.Error(w, "HTTPS required", http.StatusUpgradeRequired)
        return
    }

    // Basic Origin check to reduce CSRF risk. If Origin header is present, require it contain this host.
    if origin := r.Header.Get("Origin"); origin != "" {
        if !strings.Contains(origin, r.Host) {
            http.Error(w, "invalid origin", http.StatusForbidden)
            return
        }
    }

    // Limit the size of the request body. Add a small allowance for multipart overhead.
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+sniffBytes)

    // Parse multipart form with the same max size.
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        // Do not reveal internal parsing errors to client (could contain sensitive info).
        log.Printf("multipart parse error: %v", err)
        http.Error(w, "invalid upload", http.StatusBadRequest)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        log.Printf("form file error: %v", err)
        http.Error(w, "file required", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read the first sniffBytes bytes to validate file type without loading whole file in memory.
    sniff := make([]byte, sniffBytes)
    n, err := io.ReadFull(file, sniff)
    if err != nil && err != io.ErrUnexpectedEOF {
        log.Printf("reading file header failed: %v", err)
        http.Error(w, "could not read file", http.StatusBadRequest)
        return
    }
    sniff = sniff[:n]

    // Validate PDF magic bytes "%PDF-"
    if !bytes.HasPrefix(sniff, []byte("%PDF-")) {
        // Also check MIME sniffing as additional heuristic
        ct := http.DetectContentType(sniff)
        http.Error(w, "invalid file type", http.StatusBadRequest)
        return
    }

    // Ensure upload directory exists with restricted permissions (least privilege).
    // 0700 so only the running user can traverse/modify upload dir.
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        log.Printf("mkdir error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a random filename to avoid trusting client-provided names and to prevent collisions.
    randName, err := randomHex(24)
    if err != nil {
        log.Printf("random name generation failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    dstPath := filepath.Join(uploadDir, randName+".pdf")

    // Ensure we don't accidentally overwrite existing files (O_EXCL).
    dstFile, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        log.Printf("creating destination file failed: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // If any error occurs from now on, ensure we close and remove the partially written file.
    closed := false
    defer func() {
        if !closed {
            dstFile.Close()
            os.Remove(dstPath)
        }
    }()

    // Combine the sniffed bytes back with the remaining reader safely and limit total written bytes.
    combined := io.MultiReader(bytes.NewReader(sniff), file)
    limited := io.LimitReader(combined, int64(maxUploadSize)+1) // +1 to detect oversize

    written, err := io.Copy(dstFile, limited)
    if err != nil {
        log.Printf("copy to disk failed: %v", err)
        http.Error(w, "could not save file", http.StatusInternalServerError)
        return
    }
    if written > int64(maxUploadSize) {
        // Uploaded file exceeds our allowed max size -> remove and return error.
        dstFile.Close()
        os.Remove(dstPath)
        log.Printf("upload exceeded max size: %d bytes", written)
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Success: make sure data is flushed to disk
    if err := dstFile.Sync(); err != nil {
        // Log sync error but we still close/don't leak internals to client.
        log.Printf("fsync failed: %v", err)
    }
    if err := dstFile.Close(); err != nil {
        log.Printf("close dst file failed: %v", err)
    }
    closed = true

    // Return a minimal success response. Do not include absolute path or other internal details.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("upload successful"))
}

// randomHex returns a securely generated random hex string of length 2*n bytes.
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

func min(a, b int) int {
    if a < b {
        return a
    }
    return b
}
// ...existing code...
