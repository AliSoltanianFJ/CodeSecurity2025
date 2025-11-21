package main

import (
    "bytes"
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
    // prefer logging ListenAndServe error
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintf(os.Stderr, "server error: %v\n", err)
        os.Exit(1)
    }
}

// ...existing code...

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    // limit request body size to avoid DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large or malformed form", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // sanitize filename
    filename := filepath.Base(header.Filename)
    filename = strings.ReplaceAll(filename, string(os.PathSeparator), "_")
    if filename == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // check extension
    ext := strings.ToLower(filepath.Ext(filename))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // read file into memory (bounded by MaxBytesReader)
    data, err := io.ReadAll(file)
    if err != nil {
        http.Error(w, "failed to read uploaded file", http.StatusInternalServerError)
        return
    }

    // quick magic-number check for PDF
    if !bytes.HasPrefix(data, []byte("%PDF")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // avoid overwrite: if filename exists, prefix with timestamp
    destPath := filepath.Join(uploadDir, filename)
    if _, err := os.Stat(destPath); err == nil {
        filename = fmt.Sprintf("%d_%s", time.Now().UnixNano(), filename)
        destPath = filepath.Join(uploadDir, filename)
    }

    if err := os.WriteFile(destPath, data, 0o644); err != nil {
        http.Error(w, "failed to save uploaded file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "uploaded: %s\n", filename)
}

// ...existing code...
