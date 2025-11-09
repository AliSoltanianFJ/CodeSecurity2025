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

    // limit total upload size (10 MiB)
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read a small prefix to verify PDF magic bytes ("%PDF")
    prefix := make([]byte, 4)
    n, err := file.Read(prefix)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    if n < 4 || !strings.HasPrefix(string(prefix[:n]), "%PDF") {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // ensure extension is .pdf (from user filename) but generate our own safe name
    orig := filepath.Base(header.Filename)
    ext := strings.ToLower(filepath.Ext(orig))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // generate a collision-resistant filename (timestamp + random)
    var rnd [16]byte
    if _, err := rand.Read(rnd[:]); err != nil {
        http.Error(w, "failed to generate filename", http.StatusInternalServerError)
        return
    }
    filename := fmt.Sprintf("%d_%s%s", time.Now().UnixNano(), hex.EncodeToString(rnd[:]), ext)
    dstPath := filepath.Join(uploadDir, filename)

    // create destination file with restrictive permissions
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o600)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the prefix we already read and the rest of the file
    src := io.MultiReader(bytes.NewReader(prefix[:n]), file)
    if _, err := io.Copy(dst, src); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        // best effort: remove partial file
        _ = os.Remove(dstPath)
        return
    }

    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte(filename))
}
// ...existing code...
