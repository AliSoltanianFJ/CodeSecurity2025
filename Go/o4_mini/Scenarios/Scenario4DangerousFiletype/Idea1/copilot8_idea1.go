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
    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const maxUploadSize = 10 << 20 // 10 MB
    // limit request body size
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "invalid multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file field", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read first bytes to validate PDF magic header (%PDF-)
    magic := make([]byte, 5)
    if _, err := io.ReadFull(file, magic); err != nil {
        http.Error(w, "failed to read file header", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(magic, []byte("%PDF-")) {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // prepare reader that includes the magic bytes we already consumed
    reader := io.MultiReader(bytes.NewReader(magic), file)

    // create uploads directory
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to prepare upload directory", http.StatusInternalServerError)
        return
    }

    // generate a random filename, keep .pdf extension
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "failed to generate filename", http.StatusInternalServerError)
        return
    }
    dstName := hex.EncodeToString(rnd) + ".pdf"
    dstPath := filepath.Join(uploadDir, dstName)

    // create destination file atomically (fail if exists)
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0644)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer func() {
        dst.Close()
        // if the handler returns an error after creating the file, remove the partial file
    }()

    if _, err := io.Copy(dst, reader); err != nil {
        // remove partial file on error
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save uploaded file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte(dstName + "\n" + header.Filename))
}
