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

const (
    // maxUploadSize is the maximum size in bytes we will accept for a PDF upload.
    // Adjust as you like; here it’s ~10 MB.
    maxUploadSize = 10 << 20

    // uploadsDir is the directory where we store incoming PDFs.
    uploadsDir = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)

    fmt.Println("listening on :8080...")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintf(os.Stderr, "server error: %v\n", err)
        os.Exit(1)
    }
}

// uploadPDFHandler handles a single-file PDF upload securely.
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Disable keeping an unlimited request body around
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // 2) Parse the multipart form
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "file too big (max 10MB)", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Retrieve the file
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to get form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 4) Check the extension first (this is just a quick screen, not your only defense!)
    originalFilename := filepath.Base(header.Filename)        // sanitize any path info
    ext := strings.ToLower(filepath.Ext(originalFilename))
    if ext != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // 5) Peek at the first few bytes to verify the PDF “magic”
    //    PDF files always begin with "%PDF-"
    magic := make([]byte, 512)
    n, err := file.Read(magic)
    if err != nil && err != io.EOF {
        http.Error(w, "could not read file header: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if !bytes.HasPrefix(magic[:n], []byte("%PDF-")) {
        http.Error(w, "file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // 6) Create (if necessary) and protect your uploads directory
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        http.Error(w, "unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // 7) Generate a cryptographically-strong random filename so you never have collisions
    //    + timestamp ensures some human readability if you ever inspect uploads by hand
    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "unable to generate filename", http.StatusInternalServerError)
        return
    }
    randomHex := hex.EncodeToString(randBytes)
    newFilename := fmt.Sprintf("%d_%s.pdf", time.Now().Unix(), randomHex)
    dstPath := filepath.Join(uploadsDir, newFilename)

    // 8) Open the destination file
    dstFile, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "unable to create file on server", http.StatusInternalServerError)
        return
    }
    defer dstFile.Close()

    // 9) Re-assemble a reader that returns the bytes we “Peeked” + the rest of the file
    reader := io.MultiReader(bytes.NewReader(magic[:n]), file)

    // 10) Stream copy to disk
    if _, err := io.Copy(dstFile, reader); err != nil {
        // If something goes wrong mid-write, we should remove the partial file
        dstFile.Close()
        os.Remove(dstPath)
        http.Error(w, "error saving your file", http.StatusInternalServerError)
        return
    }

    // 11) Success!
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "upload successful: %s\n", newFilename)
}