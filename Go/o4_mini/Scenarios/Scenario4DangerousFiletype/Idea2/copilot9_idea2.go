package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // limit total upload size to 10 MiB
    const maxUploadSize = 10 << 20 // 10 MiB

    // Protect the server from large request bodies
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)

    // Parse multipart form data (small memory buffer)
    if err := r.ParseMultipartForm(32 << 10); err != nil {
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file field 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read up to a small header to validate PDF magic bytes
    header := make([]byte, 512)
    n, err := io.ReadFull(file, header)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    header = header[:n]

    // Check PDF magic header "%PDF-"
    if !bytes.HasPrefix(header, []byte("%PDF-")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with restrictive permissions
    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0o700); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generate a server-side random filename (don't trust client filename)
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rnd) + ".pdf"
    dstPath := filepath.Join(uploadsDir, filename)

    // Create destination file with restrictive permissions
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o600)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the header bytes already read, then copy the rest up to the limit
    if _, err := dst.Write(header); err != nil {
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // remaining bytes allowed (subtract bytes already read)
    remaining := int64(maxUploadSize) - int64(len(header))
    if remaining < 0 {
        _ = os.Remove(dstPath)
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Copy the rest of the upload into the file with a limit
    if _, err := io.Copy(dst, io.LimitReader(file, remaining)); err != nil {
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // Success: do not return sensitive information or original filename
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("upload successful"))
}
