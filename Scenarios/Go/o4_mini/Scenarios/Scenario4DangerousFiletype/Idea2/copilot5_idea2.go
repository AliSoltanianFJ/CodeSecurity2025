package main

import (
    "bytes"
    "crypto/rand"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // limit total upload size to 10MB
    const maxUploadSize = 10 << 20 // 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // sanitize filename: keep only a safe subset
    filename := filepath.Base(header.Filename)
    sanitizeRe := regexp.MustCompile(`[^a-zA-Z0-9._-]`)
    safeName := sanitizeRe.ReplaceAllString(filename, "_")
    // ensure non-empty and reasonable
    if safeName == "" || strings.Trim(safeName, "._-") == "" {
        // generate a random filename if original is unusable
        b := make([]byte, 8)
        if _, err := rand.Read(b); err != nil {
            http.Error(w, "server error", http.StatusInternalServerError)
            return
        }
        safeName = fmt.Sprintf("%x.pdf", b)
    }

    // enforce .pdf extension
    if strings.ToLower(filepath.Ext(safeName)) != ".pdf" {
        safeName = safeName + ".pdf"
    }

    // read first bytes to validate content-type and magic bytes
    head := make([]byte, 512)
    n, err := io.ReadFull(file, head)
    if err != nil && err != io.ErrUnexpectedEOF {
        http.Error(w, "failed to read file", http.StatusBadRequest)
        return
    }
    head = head[:n]

    // Check magic bytes for PDF and content sniffing
    if !bytes.HasPrefix(head, []byte("%PDF")) {
        // fall back to MIME sniffing; still require pdf-like type
        ct := http.DetectContentType(head)
        if !strings.HasPrefix(ct, "application/pdf") {
            http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
            return
        }
    }

    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0750); err != nil {
        http.Error(w, "failed to prepare storage", http.StatusInternalServerError)
        return
    }

    // construct safe destination path and avoid overwriting existing files
    base := strings.TrimSuffix(safeName, ".pdf")
    dstPath := filepath.Join(uploadDir, safeName)
    for i := 1; ; i++ {
        if _, err := os.Stat(dstPath); os.IsNotExist(err) {
            break
        }
        dstPath = filepath.Join(uploadDir, fmt.Sprintf("%s_%d.pdf", base, i))
    }

    // create file with restrictive permissions, fail if already exists
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0640)
    if err != nil {
        http.Error(w, "failed to store file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the already-read head then copy the rest with size limit
    if _, err := dst.Write(head); err != nil {
        http.Error(w, "failed to write file", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, io.LimitReader(file, maxUploadSize-int64(n))); err != nil {
        http.Error(w, "failed to write file", http.StatusInternalServerError)
        return
    }

    // respond without echoing user-supplied filenames or sensitive info
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("upload successful"))
}
// ...existing code...
