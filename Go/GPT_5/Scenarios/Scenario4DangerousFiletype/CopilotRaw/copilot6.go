package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintf(os.Stderr, "server error: %v\n", err)
        os.Exit(1)
    }
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // limit request body to 10MB
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    if err := r.ParseMultipartForm(10 << 20); err != nil {
        http.Error(w, "failed to parse multipart form: "+err.Error(), http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf': "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // basic filename sanitization
    origName := filepath.Base(header.Filename)
    origName = strings.TrimSpace(origName)
    if origName == "" {
        origName = "upload.pdf"
    }

    ext := strings.ToLower(filepath.Ext(origName))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // read the first few bytes to validate PDF magic number ("%PDF")
    magic := make([]byte, 4)
    n, err := io.ReadFull(file, magic)
    if err != nil && err != io.ErrUnexpectedEOF {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if n < 4 || string(magic[:n]) != "%PDF" {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        http.Error(w, "failed to create uploads dir: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // create a safe, unique filename
    ts := time.Now().UnixNano()
    dstName := fmt.Sprintf("%d_%s", ts, origName)
    dstPath := filepath.Join(uploadsDir, dstName)

    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the bytes we already read, then copy the rest
    if _, err := dst.Write(magic[:n]); err != nil {
        http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save uploaded file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "uploaded: %s\n", dstName)
}
// ...existing code...
