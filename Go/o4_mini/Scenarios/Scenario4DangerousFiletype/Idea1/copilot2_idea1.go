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
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit request size to 10MB
    const maxUploadSize = 10 << 20 // 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // Parse the multipart form (ParseMultipartForm will be called by FormFile if needed)
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read first bytes for type sniffing and then include them when writing
    sniffBuf := make([]byte, 512)
    n, err := file.Read(sniffBuf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusBadRequest)
        return
    }
    sniff := sniffBuf[:n]

    // ensure file is a PDF by magic bytes ("%PDF")
    if !bytes.HasPrefix(sniff, []byte("%PDF")) {
        // also check content-type as a secondary check
        if ct := http.DetectContentType(sniff); !strings.HasPrefix(ct, "application/pdf") {
            http.Error(w, "only PDF files are allowed", http.StatusBadRequest)
            return
        }
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // build a safe filename (ignore original name except for extension)
    dstName := fmt.Sprintf("%d.pdf", time.Now().UnixNano())
    dstPath := filepath.Join(uploadDir, dstName)

    // create destination file with exclusive create to avoid races
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o644)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer func() {
        dst.Close()
        if err != nil {
            // remove incomplete file on error
            _ = os.Remove(dstPath)
        }
    }()

    // write the sniffed bytes plus the remainder of the uploaded file
    if _, err = io.Copy(dst, io.MultiReader(bytes.NewReader(sniff), file)); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("uploaded: " + dstPath + " (orig: " + header.Filename + ")"))
}
// ...existing code...
