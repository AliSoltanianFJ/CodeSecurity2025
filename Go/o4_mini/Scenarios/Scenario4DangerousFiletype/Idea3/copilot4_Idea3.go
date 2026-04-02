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
    "time"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/*
   uploadPDFHandler saves an uploaded PDF (form field "pdf") into ./uploads,
   enforcing:
     - POST only
     - maximum upload size
     - PDF magic‐header check
     - filename sanitization + random prefix
*/
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 20 << 20 // 20 MB

    // 1) only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) limit size from the client
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "The uploaded file is too big.", http.StatusBadRequest)
        return
    }

    // 3) retrieve the file from form data
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Could not get uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 4) read a bit of the file to sniff the content type
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Could not read file header", http.StatusInternalServerError)
        return
    }
    contentType := http.DetectContentType(buf[:n])
    if contentType != "application/pdf" {
        http.Error(w, "Invalid file type, only PDFs are allowed", http.StatusBadRequest)
        return
    }

    // rewind the reader to the beginning
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Could not reset file reader", http.StatusInternalServerError)
        return
    }

    // 5) build a safe filename
    //    - strip any path components
    //    - ensure .pdf extension
    safeName := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(safeName), ".pdf") {
        safeName += ".pdf"
    }

    //    - generate a random prefix to avoid collisions
    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "Could not generate random filename", http.StatusInternalServerError)
        return
    }
    prefix := hex.EncodeToString(randBytes)

    //    - final filename and path
    filename := fmt.Sprintf("%s_%s", prefix, safeName)
    uploadDir := "./uploads"
    dstPath := filepath.Join(uploadDir, filename)

    // 6) ensure the upload directory exists
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    // 7) create destination file
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "Could not create file on disk", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 8) copy the uploaded file data to the destination file
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Could not save file", http.StatusInternalServerError)
        return
    }

    // 9) respond success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "Upload successful: %s\n", filename)
}