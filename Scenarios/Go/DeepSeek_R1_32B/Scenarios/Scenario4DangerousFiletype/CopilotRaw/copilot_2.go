package main

import (
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
    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Create uploads directory if it doesn't exist
    os.MkdirAll("./uploads", os.ModePerm)

    // Sanitize the filename to avoid path traversal attacks
    filename := filepath.Base(header.Filename)
    dst, err := os.Create("./uploads/" + filename)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy the uploaded file to destination
    if _, err = io.Copy(dst, file); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}