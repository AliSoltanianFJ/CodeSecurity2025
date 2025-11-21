// ...existing code...
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

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit upload size to 20 MB
    r.Body = http.MaxBytesReader(w, r.Body, 20<<20)

    if err := r.ParseMultipartForm(20 << 20); err != nil {
        http.Error(w, "failed to parse multipart form: "+err.Error(), http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read first bytes to detect content type
    sniff := make([]byte, 512)
    n, _ := file.Read(sniff)
    contentType := http.DetectContentType(sniff[:n])
    if !strings.HasPrefix(contentType, "application/pdf") {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // Create a safe, unique filename
    safeName := filepath.Base(header.Filename)
    filename := fmt.Sprintf("%d_%s", time.Now().UnixNano(), safeName)
    dstPath := filepath.Join(uploadDir, filename)

    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the bytes we already read, then copy the rest
    if n > 0 {
        if _, err := dst.Write(sniff[:n]); err != nil {
            http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
            return
        }
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "uploaded: %s\n", filename)
}
// ...existing code...
