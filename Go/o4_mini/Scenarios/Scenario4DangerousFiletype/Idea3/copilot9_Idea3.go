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
    // adjust as needed
    maxUploadSize = 10 << 20  // 10 MB
    uploadPath    = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // only POST allowed
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // enforce max upload size
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // parse the multipart form in the request
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "the uploaded file is too big", http.StatusRequestEntityTooLarge)
        return
    }

    // get a reference to the file
    file, _, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "could not get uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read the first 512 bytes for content sniffing
    head := make([]byte, 512)
    n, err := io.ReadFull(file, head)
    if err != nil && err != io.ErrUnexpectedEOF {
        http.Error(w, "could not read file header", http.StatusInternalServerError)
        return
    }
    head = head[:n]

    // verify PDF magic number
    // PDF files always start with "%PDF-"
    if !bytes.HasPrefix(head, []byte("%PDF-")) {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // rebuild a reader that starts with the bytes we just read
    reader := io.MultiReader(bytes.NewReader(head), file)

    // ensure upload directory exists
    if err := os.MkdirAll(uploadPath, 0755); err != nil {
        http.Error(w, "could not create upload directory", http.StatusInternalServerError)
        return
    }

    // generate a random filename so we never collide or allow directory traversal
    randomName, err := generateRandomFilename(16)
    if err != nil {
        http.Error(w, "could not generate filename", http.StatusInternalServerError)
        return
    }
    dstPath := filepath.Join(uploadPath, randomName+".pdf")

    // create the destination file (O_EXCL to fail if the file somehow already exists)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "could not create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // copy the entirety of the upload to disk
    if _, err := io.Copy(dst, reader); err != nil {
        // if something goes wrong during write, remove partial file
        dst.Close()
        os.Remove(dstPath)
        http.Error(w, "could not save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("upload successful\n"))
}

// generateRandomFilename returns a hex-encoded random string of length 2*n
// e.g. n=16 → 32 hex characters
func generateRandomFilename(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}