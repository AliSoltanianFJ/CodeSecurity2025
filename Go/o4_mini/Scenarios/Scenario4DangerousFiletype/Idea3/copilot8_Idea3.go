package main

import (
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
    // maxUploadSize is the maximum size in bytes of the file we’ll accept.
    // Here: 10 MiB
    maxUploadSize = 10 << 20
    // uploadPath is the folder where we'll store uploads.
    uploadPath = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Require POST
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Limit the size of the request body
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // 3) Parse the multipart form in the request
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 4) Retrieve the file from form data
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file “pdf”", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 5) Check extension
    origFilename := header.Filename
    ext := strings.ToLower(filepath.Ext(origFilename))
    if ext != ".pdf" {
        http.Error(w, "invalid file type: only .pdf allowed", http.StatusBadRequest)
        return
    }

    // 6) Read a peek of the first 512 bytes to verify content-type
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "could not read file header", http.StatusInternalServerError)
        return
    }
    mimeType := http.DetectContentType(buf[:n])
    if mimeType != "application/pdf" {
        http.Error(w, "invalid file type: not a PDF", http.StatusBadRequest)
        return
    }
    // rewind back to start
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "could not seek file", http.StatusInternalServerError)
        return
    }

    // 7) Sanitize the original filename (strip any path components)
    name := strings.TrimSuffix(filepath.Base(origFilename), ext)
    if name == "" {
        name = "upload"
    }

    // 8) Generate a cryptographically random filename suffix
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "could not generate filename", http.StatusInternalServerError)
        return
    }
    finalName := fmt.Sprintf("%s_%s%s", name, hex.EncodeToString(rnd), ext)

    // 9) Ensure upload directory exists
    if err := os.MkdirAll(uploadPath, 0755); err != nil {
        http.Error(w, "could not create upload directory", http.StatusInternalServerError)
        return
    }

    // 10) Create destination file
    dst, err := os.Create(filepath.Join(uploadPath, finalName))
    if err != nil {
        http.Error(w, "could not create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 11) Copy the uploaded file to the filesystem
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "could not save file", http.StatusInternalServerError)
        return
    }

    // 12) Success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "File uploaded successfully: %s\n", finalName)
}