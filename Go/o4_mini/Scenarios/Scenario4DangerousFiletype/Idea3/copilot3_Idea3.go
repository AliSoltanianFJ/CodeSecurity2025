package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    // allow up to 10 MB uploads
    maxUploadSize = 10 << 20 // 10 MB
    uploadPath    = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

// uploadPDFHandler handles PDF uploads.
//
// It enforces:
//  - POST-only
//  - a maximum size via http.MaxBytesReader
//  - .pdf extension + PDF magic‐number check
//  - a randomly generated filename in ./uploads
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) enforce a maximum upload size
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // 3) parse the multipart form in-memory up to maxUploadSize
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "the uploaded file is too big", http.StatusBadRequest)
        return
    }

    // 4) retrieve the file
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "invalid form field “pdf”", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 5) check the extension
    if ext := strings.ToLower(filepath.Ext(header.Filename)); ext != ".pdf" {
        http.Error(w, "invalid file type", http.StatusBadRequest)
        return
    }

    // 6) read the first few bytes to verify the PDF magic‐number
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "could not read file", http.StatusInternalServerError)
        return
    }
    if !bytes.HasPrefix(buf[:n], []byte("%PDF-")) {
        http.Error(w, "invalid PDF file", http.StatusBadRequest)
        return
    }

    // 7) rewind the file reader
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "could not seek in file", http.StatusInternalServerError)
        return
    }

    // 8) ensure the upload directory exists
    if err := os.MkdirAll(uploadPath, 0755); err != nil {
        http.Error(w, "could not create upload directory", http.StatusInternalServerError)
        return
    }

    // 9) generate a random filename (16 bytes → 32 hex chars) + .pdf
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "could not generate filename", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rnd) + ".pdf"
    dstPath := filepath.Join(uploadPath, filename)

    // 10) create & copy to the destination file
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "could not create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "could not save file", http.StatusInternalServerError)
        return
    }

    // 11) success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "upload successful: %s\n", filename)
}