// ...existing code...
package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "mime"
    "mime/multipart"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions:
    // - Enforce POST and multipart/form-data
    // - Enforce a strict maximum upload size using http.MaxBytesReader to mitigate DoS
    // - Validate PDF magic bytes ("%PDF-") to avoid content-type spoofing
    // - Never use client-supplied filename (prevents path traversal); generate a random filename
    // - Create uploads directory with restrictive permissions and write file with 0600
    // - Use O_EXCL to avoid overwriting existing files
    // - Do not leak internal errors to clients; log server-side details
    const maxUploadSize = 10 << 20 // 10 MiB
    const sniffLen = 512           // bytes to read for content sniffing

    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require multipart/form-data content type
    ct := r.Header.Get("Content-Type")
    if ct == "" {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }
    mediatype, _, err := mime.ParseMediaType(ct)
    if err != nil || mediatype != "multipart/form-data" {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }

    // Limit total request size to mitigate memory/CPU/IO abuse
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+int64(sniffLen))
    // Parse multipart form with a small in-memory buffer; files will be streamed to disk/temporary storage by the library
    if err := r.ParseMultipartForm(32 << 10); err != nil {
        // Do not reveal internal parsing errors to client
        log.Printf("upload: failed to parse multipart form: %v", err)
        http.Error(w, "failed to process upload", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        if err == http.ErrMissingFile || err == multipart.ErrMessageTooLarge {
            http.Error(w, "invalid upload", http.StatusBadRequest)
            return
        }
        log.Printf("upload: FormFile error: %v", err)
        http.Error(w, "failed to process upload", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read initial bytes for magic-byte/content sniffing without losing them
    head := make([]byte, sniffLen)
    n, readErr := io.ReadFull(file, head)
    if readErr != nil && readErr != io.ErrUnexpectedEOF && readErr != io.EOF {
        log.Printf("upload: failed reading file head: %v", readErr)
        http.Error(w, "failed to process upload", http.StatusBadRequest)
        return
    }
    head = head[:n]

    // Validate PDF magic bytes: PDFs start with "%PDF-"
    if !bytes.HasPrefix(head, []byte("%PDF-")) {
        // Reject if magic bytes don't match
        log.Printf("upload: rejected non-pdf upload (filename=%q, first=%q)", header.Filename, string(head))
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Create uploads directory with restrictive permissions if it doesn't exist
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        log.Printf("upload: cannot create upload dir: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a secure random filename. Do NOT use client-supplied filename.
    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        log.Printf("upload: cannot generate filename: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(randBytes) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // Create destination file with exclusive create and restrictive permissions
    dstFile, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        log.Printf("upload: cannot create destination file: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        // Ensure the file is closed; in case of failure below, remove it to avoid partially written files
        if cerr := dstFile.Close(); cerr != nil {
            log.Printf("upload: error closing file: %v", cerr)
        }
    }()

    // Reconstruct reader: first the head we already consumed, then the rest of the file
    reader := io.MultiReader(bytes.NewReader(head), file)

    // Copy the data into destination file. The request body is protected by MaxBytesReader.
    written, copyErr := io.Copy(dstFile, reader)
    if copyErr != nil {
        // Remove partial file
        _ = os.Remove(dstPath)
        // If the client exceeded the size limit, MaxBytesReader will cause an error here
        log.Printf("upload: failed to save file (written=%d): %v", written, copyErr)
        http.Error(w, "failed to save upload", http.StatusBadRequest)
        return
    }

    // Final sanity check on size (defense-in-depth)
    if written == 0 || written > int64(maxUploadSize) {
        _ = os.Remove(dstPath)
        log.Printf("upload: invalid filesize after save (written=%d)", written)
        http.Error(w, "invalid file size", http.StatusBadRequest)
        return
    }

    // Successful upload. Return safe, generic response. Do not return internal paths.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("upload successful"))
    log.Printf("upload: saved %q (%d bytes) from form field 'pdf' as %q at %s", header.Filename, written, filename, time.Now().Format(time.RFC3339))
}
// ...existing code...
