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

const (
    // uploadDir is where we store all incoming PDFs
    uploadDir = "./uploads"

    // maxUploadSize is the maximum size in bytes of an incoming PDF (10 MiB)
    maxUploadSize = 10 << 20
)

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only allow POST
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) prevent a client from sending a body larger than our limit
    //    any Read beyond this limit will return an error
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // 3) parse the multipart form in the request
    //
    //    Note: because we wrapped r.Body with MaxBytesReader,
    //    calls to ParseMultipartForm will error out if the total
    //    body size exceeds maxUploadSize.
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "file too large or malformed form", http.StatusBadRequest)
        return
    }

    // 4) retrieve the file from form data
    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form field 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 5) peek at the first few bytes to make sure it’s a PDF
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "unable to read file header", http.StatusInternalServerError)
        return
    }
    if !bytes.HasPrefix(buf[:n], []byte("%PDF-")) {
        http.Error(w, "invalid file type: only PDF allowed", http.StatusBadRequest)
        return
    }

    // 6) rewind the reader so we can re-copy the entire file
    if seeker, ok := file.(io.Seeker); ok {
        if _, err := seeker.Seek(0, io.SeekStart); err != nil {
            http.Error(w, "unable to seek file", http.StatusInternalServerError)
            return
        }
    } else {
        // in the unlikely event multipart.File isn't seekable, re-prepend what we read
        file = io.MultiReader(bytes.NewReader(buf[:n]), file)
    }

    // 7) ensure the upload directory exists
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // 8) generate a collision-resistant random filename
    var rnd [16]byte
    if _, err := rand.Read(rnd[:]); err != nil {
        http.Error(w, "unable to generate filename", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rnd[:]) + ".pdf"
    dstPath := filepath.Join(uploadDir, filename)

    // 9) atomically create the destination file (fail if it already exists)
    dst, err := os.OpenFile(dstPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0644)
    if err != nil {
        http.Error(w, "unable to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 10) copy the entire contents into our new file
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }

    // 11) success
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("upload successful"))
}

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}