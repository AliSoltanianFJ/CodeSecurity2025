// ...existing code...
package main

import (
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
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

    // sanitize filename and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // read a few bytes to check PDF magic header
    buf := make([]byte, 4)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read file header: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if n < 4 || !strings.HasPrefix(string(buf[:n]), "%PDF") {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    if err := os.MkdirAll("./uploads", 0755); err != nil {
        http.Error(w, "failed to create uploads directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    outPath := filepath.Join("uploads", filename)
    outFile, err := os.Create(outPath)
    if err != nil {
        http.Error(w, "failed to create file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer outFile.Close()

    // write the bytes already read, then copy the rest
    if _, err := outFile.Write(buf[:n]); err != nil {
        http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(outFile, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("file uploaded"))
}
// ...existing code...
