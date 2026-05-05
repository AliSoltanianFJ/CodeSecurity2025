package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

const (
    // 10 MiB
    maxUploadSize = 10 << 20
    uploadPath    = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    fmt.Println("Listening on :8080…")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
    }
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Limit the size of the request body to prevent DOS via large uploads
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // 2) Parse the multipart form in the request
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "The uploaded file is too big. Please choose an under 10MB file", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Retrieve file from posted form-data
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Could not read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 4) Sanitize and validate filename
    filename := filepath.Base(header.Filename) // strips any path components
    if !strings.EqualFold(filepath.Ext(filename), ".pdf") {
        http.Error(w, "Invalid file extension. Only .pdf is allowed", http.StatusBadRequest)
        return
    }

    // 5) Read first 512 bytes for content sniffing
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Unable to read file for content check", http.StatusInternalServerError)
        return
    }

    // 6) Check PDF magic bytes (`%PDF-`)
    if !isPDF(buf[:n]) {
        http.Error(w, "File is not a valid PDF", http.StatusBadRequest)
        return
    }

    // 7) Reset read pointer so we can write the entire file out
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Unable to reset file pointer", http.StatusInternalServerError)
        return
    }

    // 8) Ensure upload directory exists
    if err := os.MkdirAll(uploadPath, 0o755); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    // 9) Generate a collision-resistant filename
    uniqueName, err := generateFilename(filename)
    if err != nil {
        http.Error(w, "Could not generate filename", http.StatusInternalServerError)
        return
    }

    destPath := filepath.Join(uploadPath, uniqueName)
    dst, err := os.Create(destPath)
    if err != nil {
        http.Error(w, "Unable to create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 10) Stream the file to disk
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // 11) Success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "File uploaded successfully: %s\n", uniqueName)
}

// isPDF does a simple check for PDF magic bytes.
// Most PDFs start with "%PDF-"
// We only need the first 4 or 5 bytes.
func isPDF(b []byte) bool {
    return len(b) >= 4 && string(b[:4]) == "%PDF"
}

// generateFilename returns a filesystem-safe, collision-resistant name.
// We prepend a timestamp and a random hex string to the sanitized base name.
func generateFilename(orig string) (string, error) {
    // strip any path elements (just a sanity check; we already ran filepath.Base)
    base := filepath.Base(orig)

    // get a little entropy
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        return "", err
    }

    // timestamp + random hex + original name
    return fmt.Sprintf("%d-%s-%s",
        time.Now().UnixNano(),
        hex.EncodeToString(rnd),
        base,
    ), nil
}