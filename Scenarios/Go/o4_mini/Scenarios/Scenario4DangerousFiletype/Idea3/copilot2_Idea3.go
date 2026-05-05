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
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    const (
        maxUploadSize = 10 << 20 // 10 MiB
        uploadPath    = "./uploads"
    )

    // 1) Only allow POST
    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Limit the size of the request body to avoid DOS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)

    // 3) Parse the multipart form in the request
    //    We pass maxUploadSize here so that ParseMultipartForm
    //    will also error if the file part is bigger.
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "The uploaded file is too big. Please use a file under 10MB", http.StatusRequestEntityTooLarge)
        return
    }

    // 4) Retrieve the file from form data
    file, fileHeader, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Could not read uploaded file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // 5) Read the first 512 bytes for content sniffing
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "Could not read file for type validation", http.StatusBadRequest)
        return
    }

    // 6) Verify the file really is a PDF
    contentType := http.DetectContentType(buf[:n])
    if contentType != "application/pdf" {
        http.Error(w, "Invalid file type: only PDFs are allowed", http.StatusBadRequest)
        return
    }

    // 7) Make sure the uploads folder exists
    if err := os.MkdirAll(uploadPath, 0755); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    // 8) Sanitize the original filename and ensure it ends in .pdf
    origFilename := filepath.Base(fileHeader.Filename)
    ext := strings.ToLower(filepath.Ext(origFilename))
    if ext != ".pdf" {
        ext = ".pdf"
    }

    // 9) Generate a unique, random filename so we don’t collide
    rnd := make([]byte, 16)
    if _, err := rand.Read(rnd); err != nil {
        http.Error(w, "Could not generate file name", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(rnd) + ext
    dstPath := filepath.Join(uploadPath, filename)

    // 10) Create the destination file
    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "Could not create destination file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // 11) Write the 512 bytes we already read
    if _, err := dst.Write(buf[:n]); err != nil {
        http.Error(w, "Could not write to destination file", http.StatusInternalServerError)
        return
    }

    // 12) Copy the remainder of the file
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Could not save the rest of the file", http.StatusInternalServerError)
        return
    }

    // 13) Success!
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "Upload successful. Stored as %s\n", filename)
}