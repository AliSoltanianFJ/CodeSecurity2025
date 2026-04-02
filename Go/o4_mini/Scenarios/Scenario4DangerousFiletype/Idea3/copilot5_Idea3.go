package main

import (
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
    // 10 megabytes
    maxUploadSize = 10 * 1024 * 1024
    uploadPath    = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    fmt.Println("Listening on :8080...")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintf(os.Stderr, "Server error: %v\n", err)
        os.Exit(1)
    }
}

// uploadPDFHandler handles PDF uploads and saves them securely to disk.
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Limit the size of the request to avoid denial of service / memory exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "The uploaded file is too big. Please use a file less than 10MB in size.", http.StatusRequestEntityTooLarge)
        return
    }

    // 2) Retrieve the file from form data
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Could not get uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 3) Read a chunk to detect the content type
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Could not read file", http.StatusInternalServerError)
        return
    }

    contentType := http.DetectContentType(buf[:n])
    if contentType != "application/pdf" {
        http.Error(w, "Invalid file type. Please upload a PDF.", http.StatusBadRequest)
        return
    }

    // 4) Reset the read pointer if the file is seekable
    if seeker, ok := file.(io.Seeker); ok {
        _, err = seeker.Seek(0, io.SeekStart)
        if err != nil {
            http.Error(w, "Could not reset file pointer", http.StatusInternalServerError)
            return
        }
    } else {
        // multipart.File should generally be seekable; if not, we need to re-open
        http.Error(w, "Cannot process uploaded file", http.StatusInternalServerError)
        return
    }

    // 5) Sanitize the filename
    origFilename := filepath.Base(header.Filename)
    ext := strings.ToLower(filepath.Ext(origFilename))
    if ext != ".pdf" {
        http.Error(w, "Invalid file extension; only .pdf is allowed", http.StatusBadRequest)
        return
    }

    // 6) Generate a unique filename to avoid collisions and guessing
    uniqueName, err := randomFilename(16)
    if err != nil {
        http.Error(w, "Could not generate filename", http.StatusInternalServerError)
        return
    }
    filename := fmt.Sprintf("%s_%d%s", uniqueName, time.Now().UnixNano(), ext)
    dstPath := filepath.Join(uploadPath, filename)

    // 7) Make sure the upload directory exists
    if err := os.MkdirAll(uploadPath, 0755); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    // 8) Create the destination file
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Could not save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 9) Copy the uploaded file data to the destination file
    if _, err := io.Copy(dst, file); err != nil {
        // Attempt to remove a partially written file
        os.Remove(dstPath)
        http.Error(w, "Could not write file to disk", http.StatusInternalServerError)
        return
    }

    // 10) Success!
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully as " + filename))
}

// randomFilename returns a securely generated random hex string of length byteCount*2.
// e.g. byteCount=16 yields a 32-character hex string.
func randomFilename(byteCount int) (string, error) {
    b := make([]byte, byteCount)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}