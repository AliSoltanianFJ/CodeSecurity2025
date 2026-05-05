package main

import (
    "bytes"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit total request size (including multipart overhead)
    const maxUploadSize = 10 << 20 // 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)

    // read uploaded file
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // basic filename sanitization (do not use the client filename for storage)
    _ = filepath.Base(header.Filename) // keep to acknowledge the value if needed later

    // read first bytes to verify PDF magic header
    magic := make([]byte, 4)
    n, err := file.Read(magic)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file", http.StatusInternalServerError)
        return
    }
    if n < 4 || !bytes.HasPrefix(bytes.ToLower(magic[:n]), []byte("%pdf")) {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to prepare upload directory", http.StatusInternalServerError)
        return
    }

    // create a secure temporary file name and write contents atomically
    tmp, err := os.CreateTemp(uploadDir, "upload-*.pdf")
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer func() {
        tmp.Close()
    }()

    // write the bytes we already read, then the remainder
    if _, err := tmp.Write(magic[:n]); err != nil {
        os.Remove(tmp.Name())
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(tmp, file); err != nil {
        os.Remove(tmp.Name())
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // tighten permissions
    if err := os.Chmod(tmp.Name(), 0o600); err != nil {
        // non-fatal: continue but remove on failure to be safe
        os.Remove(tmp.Name())
        http.Error(w, "failed to finalize file", http.StatusInternalServerError)
        return
    }

    // respond with the stored filename (not the original client path)
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte(strings.TrimPrefix(tmp.Name(), "./")))
}
