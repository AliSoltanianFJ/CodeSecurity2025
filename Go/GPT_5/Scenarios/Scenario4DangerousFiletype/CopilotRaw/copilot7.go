// ...existing code...
package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read first bytes to verify PDF magic header "%PDF"
    sig := make([]byte, 4)
    n, err := file.Read(sig)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if n < 4 || string(sig[:4]) != "%PDF" {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    if err := os.MkdirAll("./uploads", 0755); err != nil {
        http.Error(w, "failed to create uploads directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // Sanitize filename and create destination file
    fname := filepath.Base(header.Filename)
    dstPath := filepath.Join("uploads", fname)

    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Write the bytes we've already read, then copy the remainder
    if _, err := dst.Write(sig[:n]); err != nil {
        http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "uploaded: %s", dstPath)
}
