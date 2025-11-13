package main

import (
    "io"
    "os"
    "path/filepath"
    "net/http"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Create uploads directory if not exists
    os.MkdirAll("./uploads", 0755)

    // Save the uploaded file to ./uploads directory
    dstPath := filepath.Join("./uploads", header.Filename)
    dstFile, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE, 0644)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer dstFile.Close()

    // Copy file contents from the request to the destination file
    if _, err = io.Copy(dstFile, file); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}