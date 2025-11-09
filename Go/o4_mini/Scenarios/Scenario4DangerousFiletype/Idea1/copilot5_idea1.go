package main

import (
    "bytes"
    "crypto/rand"
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
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 20 << 20 // 20 MB

    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // enforce size limit
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // parse multipart form (small memory buffer)
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        http.Error(w, "failed to parse multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read first bytes to validate PDF magic bytes
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file", http.StatusBadRequest)
        return
    }
    if n < 4 || string(buf[:4]) != "%PDF" {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // prepare reader that includes the bytes we already read
    reader := io.MultiReader(bytes.NewReader(buf[:n]), file)

    // sanitize filename and require .pdf extension
    filename := filepath.Base(header.Filename)
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // create a unique destination filename
    var rnd [8]byte
    if _, err := rand.Read(rnd[:]); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    dstPath := filepath.Join(uploadDir, fmt.Sprintf("%d_%x_%s", time.Now().UnixNano(), rnd, filename))

    // create file (fail if exists)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // copy uploaded content to destination
    if _, err := io.Copy(dst, reader); err != nil {
        _ = dst.Close()
        _ = os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded: " + dstPath))
}
