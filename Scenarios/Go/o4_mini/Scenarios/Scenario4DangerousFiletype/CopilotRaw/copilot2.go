package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    file, header, err := r.FormFile("pdf")
    //-copilot next line-
    if err != nil {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // sanitize filename and ensure it is a PDF
    filename := filepath.Base(header.Filename)
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // add a timestamp prefix to avoid collisions
    outName := fmt.Sprintf("%d_%s", time.Now().UnixNano(), filename)
    outPath := filepath.Join(uploadDir, outName)

    outFile, err := os.Create(outPath)
    if err != nil {
        http.Error(w, "failed to create file on server: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer outFile.Close()

    if _, err := io.Copy(outFile, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "uploaded")
}
// ...existing code...
