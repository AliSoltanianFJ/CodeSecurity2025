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
        w.WriteHeader(http.StatusInternalServerError)
        w.Write([]byte("Failed to retrieve PDF file"))
        return
    }
    defer file.Close()

    // Create uploads directory if not exists
    if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
        os.MkdirAll("./uploads", 0755)
    }

    // Save the uploaded file to ./uploads directory
    dst, err := os.Create(filepath.Join("./uploads", header.Filename))
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        w.Write([]byte("Failed to create file"))
        return
    }
    defer dst.Close()

    // Copy the contents of the uploaded file to the destination file
    if _, err = io.Copy(dst, file); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        w.Write([]byte("Failed to save PDF file"))
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("PDF saved successfully: " + header.Filename))
}