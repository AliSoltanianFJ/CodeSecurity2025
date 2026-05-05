// ...existing code...
package main

import (
    "bytes"
    "fmt"
    "html"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "time"
)

const maxUploadSize = 10 << 20 // 10 MB

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request body to avoid large uploads and possible memory exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512)

    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large or malformed form", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf form file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read first bytes to determine content type and check PDF magic header
    peek := make([]byte, 512)
    n, err := file.Read(peek)
    if err != nil && err != io.EOF {
        http.Error(w, "unable to read uploaded file", http.StatusInternalServerError)
        return
    }
    peek = peek[:n]

    // Verify content type using server-side detection (don't trust client-provided value)
    contentType := http.DetectContentType(peek)
    if contentType != "application/pdf" {
        http.Error(w, "uploaded file is not a valid PDF (bad content type)", http.StatusUnsupportedMediaType)
        return
    }

    // Verify PDF magic bytes
    if !bytes.HasPrefix(peek, []byte("%PDF-")) {
        http.Error(w, "uploaded file is not a valid PDF (invalid header)", http.StatusUnsupportedMediaType)
        return
    }

    // Prepare uploads directory
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o700); err != nil {
        http.Error(w, "unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // Create a safe temporary file name inside uploads directory (avoids using user filename)
    tmpFile, err := os.CreateTemp(uploadDir, "upload-*.pdf")
    if err != nil {
        http.Error(w, "unable to create file on server", http.StatusInternalServerError)
        return
    }
    defer tmpFile.Close()

    // Write the bytes we already read, then copy the remainder up to the maximum allowed size
    if _, err := tmpFile.Write(peek); err != nil {
        http.Error(w, "failed writing to file", http.StatusInternalServerError)
        return
    }
    remaining := int64(maxUploadSize) - int64(len(peek))
    if remaining < 0 {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }
    if _, err := io.Copy(tmpFile, io.LimitReader(file, remaining)); err != nil {
        http.Error(w, "failed writing to file", http.StatusInternalServerError)
        return
    }

    // Restrict file permissions (best-effort; Windows ignores Unix perms)
    _ = tmpFile.Chmod(0o600)

    // Do not reveal server paths or original client paths. Only return the generated filename.
    safeName := html.EscapeString(filepath.Base(tmpFile.Name()))
    origName := html.EscapeString(filepath.Base(header.Filename))

    // Minimal, safe response
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintf(w, "uploaded=%s\noriginal_filename=%s\ntimestamp=%s\n", safeName, origName, time.Now().UTC().Format(time.RFC3339))
}
// ...existing code...
