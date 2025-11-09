// ...existing code...
package main

import (
    "bytes"
    "crypto/rand"
    "crypto/sha256"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "fmt"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // NOTE: for real deployments, do NOT use ListenAndServe without TLS.
    // Use ListenAndServeTLS or put the service behind a TLS-terminating proxy.
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security design decisions (brief):
    // - Enforce POST only.
    // - Limit maximum body size to avoid DoS & disk exhaustion.
    // - Validate filename extension and magic bytes (%PDF-) to mitigate dangerous filetypes/RCE.
    // - Use a cryptographically-random filename to avoid path traversal and collisions.
    // - Create uploads directory with restricted permissions.
    // - Write file with O_EXCL and 0600 permissions.
    // - Do not echo internal errors to clients; log details server-side.
    // - Recommend TLS for transport (see main()).

    const maxFileSize = 10 << 20 // 10 MiB

    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Prevent the client from sending an arbitrarily large request.
    r.Body = http.MaxBytesReader(w, r.Body, maxFileSize+1024) // small overhead for multipart framing

    // Parse multipart form (memory limit, rest goes to temporary files).
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        // don't leak internal parse errors
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename sanitization
    origName := filepath.Base(header.Filename)
    if origName == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    ext := strings.ToLower(filepath.Ext(origName))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // If the client declared a size, check it early.
    if header.Size > maxFileSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Read the first few bytes to verify PDF magic bytes ("%PDF-")
    const pdfMagicLen = 5
    magic := make([]byte, pdfMagicLen)
    n, err := io.ReadFull(file, magic)
    if err != nil && err != io.ErrUnexpectedEOF {
        // Could not read enough bytes to validate
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if n < pdfMagicLen || !bytes.HasPrefix(magic[:n], []byte("%PDF-")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with restrictive permissions.
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o700); err != nil {
        log.Printf("mkdir error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a cryptographically secure random filename (no user input used)
    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        log.Printf("rand read error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    tmpName := hex.EncodeToString(randBytes) + ".pdf"
    outPath := filepath.Join(uploadDir, tmpName)

    // Create destination file with O_EXCL to avoid races, and restrictive perms.
    outFile, err := os.OpenFile(outPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0o600)
    if err != nil {
        log.Printf("openfile error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        if cerr := outFile.Close(); cerr != nil {
            log.Printf("close error: %v", cerr)
        }
    }()

    // Prepare hashing and write initial bytes already read.
    hasher := sha256.New()
    multi := io.MultiWriter(outFile, hasher)

    // Write the bytes we already read.
    if _, err := multi.Write(magic[:n]); err != nil {
        log.Printf("write error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        // best-effort cleanup
        _ = os.Remove(outPath)
        return
    }

    // Copy the remainder of the file up to maxFileSize.
    remaining := maxFileSize - int64(n)
    limited := &io.LimitedReader{R: file, N: remaining}
    if _, err := io.Copy(multi, limited); err != nil {
        log.Printf("copy error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        _ = os.Remove(outPath)
        return
    }
    // If the client tried to send more than allowed, N will reach 0 but the underlying
    // MaxBytesReader will trigger on the next read; ensure we didn't overflow.
    if limited.N == 0 {
        // Attempt to read one more byte to detect overflow; ignore error details.
        var probe [1]byte
        if _, err := file.Read(probe[:]); err == nil {
            // there was more data beyond the allowed limit
            _ = os.Remove(outPath)
            http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
            return
        }
    }

    // Optionally record metadata: original filename, uploader IP, sha256, etc.
    sum := hasher.Sum(nil)
    hashHex := hex.EncodeToString(sum)
    metaPath := outPath + ".meta"
    metaFile, err := os.OpenFile(metaPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o600)
    if err == nil {
        fmt.Fprintf(metaFile, "original_filename=%s\nsha256=%s\nuploader_ip=%s\n", origName, hashHex, r.RemoteAddr)
        metaFile.Close()
    } else {
        // Non-fatal; log for auditing. Do not leak to client.
        log.Printf("could not write metadata: %v", err)
    }

    // Success: return minimal, non-sensitive confirmation.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprint(w, "upload successful")
}
// ...existing code...
