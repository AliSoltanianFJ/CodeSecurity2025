// ...existing code...
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

    // limit upload size (10 MiB)
    const maxUploadSize = 10 << 20
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read a small prefix to validate PDF magic bytes
    prefix := make([]byte, 512)
    n, err := io.ReadFull(file, prefix)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, "failed to read file", http.StatusBadRequest)
        return
    }
    // PDF files start with "%PDF-"
    if n < 5 || string(prefix[:5]) != "%PDF-" {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // create uploads dir if missing
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // generate a cryptographically random filename to avoid collisions and avoid using client filename
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rnd) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // open destination file with O_EXCL to avoid races
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o644)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write back the prefix we read, then the rest of the file
    reader := io.MultiReader(bytes.NewReader(prefix[:n]), file)
    if _, err := io.Copy(dst, reader); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded: " + filename))
}
// ...existing code...
