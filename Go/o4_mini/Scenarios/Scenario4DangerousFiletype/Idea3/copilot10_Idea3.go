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

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const (
        maxUploadSize = 10 << 20  // 10MB
        uploadPath    = "./uploads"
    )

    // 1) only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) reject requests bigger than our maxUploadSize
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "Uploaded file is too big", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) grab the file from the form data
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid form field 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 4) check extension
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // 5) peek at the first 512 bytes to detect content‐type
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Failed to read file", http.StatusInternalServerError)
        return
    }
    detected := http.DetectContentType(buf[:n])
    if detected != "application/pdf" {
        http.Error(w, "File content is not PDF", http.StatusBadRequest)
        return
    }

    // rewind the reader back to start
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Could not seek file", http.StatusInternalServerError)
        return
    }

    // 6) create uploads folder if it doesn't exist
    if err := os.MkdirAll(uploadPath, 0755); err != nil {
        http.Error(w, "Unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // 7) sanitize / rewrite filename
    //    here we just combine a timestamp + the .pdf extension
    fname := fmt.Sprintf("%d%s", time.Now().UnixNano(), ext)
    dstPath := filepath.Join(uploadPath, fname)

    // 8) create the file on disk
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "Unable to create file on disk", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 9) copy the uploaded file straight to disk
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Unable to save file", http.StatusInternalServerError)
        return
    }

    // 10) success!
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "Upload successful: %s\n", fname)
}