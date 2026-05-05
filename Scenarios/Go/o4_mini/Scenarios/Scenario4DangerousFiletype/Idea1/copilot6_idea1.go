package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
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

    // limit total request size (including multipart overhead) to 10MB
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20) // 10 MiB

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // check filename safe base and extension
    origName := filepath.Base(header.Filename)
    if origName == "." || origName == string(filepath.Separator) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }
    if strings.ToLower(filepath.Ext(origName)) != ".pdf" {
        http.Error(w, "only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // read a small prefix to validate PDF magic bytes
    prefix := make([]byte, 512)
    n, _ := file.Read(prefix)
    if n == 0 {
        http.Error(w, "empty file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(prefix[:n], []byte("%PDF")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // generate a collision-resistant filename and create file atomically
    var dst *os.File
    var dstPath string
    for i := 0; i < 5; i++ {
        rnd := make([]byte, 16)
        if _, err := rand.Read(rnd); err != nil {
            http.Error(w, "internal error", http.StatusInternalServerError)
            return
        }
        newName := hex.EncodeToString(rnd) + ".pdf"
        dstPath = filepath.Join(uploadDir, newName)
        dst, err = os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
        if err == nil {
            break
        }
        if !os.IsExist(err) {
            http.Error(w, "failed to create destination file", http.StatusInternalServerError)
            return
        }
    }
    if dst == nil {
        http.Error(w, "could not create a unique destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // copy the prefix we already read plus the remainder
    if _, err := io.Copy(dst, io.MultiReader(bytes.NewReader(prefix[:n]), file)); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        // best-effort cleanup
        _ = os.Remove(dstPath)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded: " + dstPath))
}
// ...existing code...
