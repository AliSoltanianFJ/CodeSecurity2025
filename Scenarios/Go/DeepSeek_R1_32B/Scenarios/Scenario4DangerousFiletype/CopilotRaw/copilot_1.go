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
    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Create uploads directory if it doesn't exist
    if err := os.MkdirAll("./uploads", 0755); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    // Save the uploaded file to ./uploads directory
    filePath := filepath.Join("./uploads", header.Filename)
    out, err := os.Create(filePath)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer out.Close()

    _, err = io.Copy(out, file)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "File uploaded successfully to %s", filePath)
}