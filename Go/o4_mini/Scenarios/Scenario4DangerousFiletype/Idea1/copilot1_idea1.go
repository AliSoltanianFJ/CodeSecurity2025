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

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const maxUploadSize = 10 << 20 // 10 MB

    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit total size and parse multipart form
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "request too large", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read first bytes to verify PDF magic/MIME
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file header", http.StatusBadRequest)
        return
    }

    // check for PDF magic or MIME sniffing
    if !bytes.HasPrefix(buf[:n], []byte("%PDF")) {
        if ct := http.DetectContentType(buf[:n]); !strings.HasPrefix(ct, "application/pdf") {
            http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
            return
        }
    }

    // prepare reader that includes the bytes we already read
    var src io.Reader
    if seeker, ok := file.(io.Seeker); ok {
        if _, err := seeker.Seek(0, io.SeekStart); err == nil {
            src = file
        } else {
            src = io.MultiReader(bytes.NewReader(buf[:n]), file)
        }
    } else {
        src = io.MultiReader(bytes.NewReader(buf[:n]), file)
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // generate a safe, unique filename (do not use user-supplied name directly)
    id := make([]byte, 16)
    if _, err := rand.Read(id); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(id) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // create destination file (use O_EXCL to avoid overwrite)
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, src); err != nil {
        // remove partial file on error
        dst.Close()
        os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte(filename + "\n"))
}

// ...existing code...
