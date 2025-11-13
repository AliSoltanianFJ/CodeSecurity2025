package main

import (
    "io"
    "log"
    "net/http"
    "os"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    file, header, err := r.FormFile("pdf")
    if err != nil {
        log.Println("Error retrieving file:", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Create uploads directory if it doesn't exist
    if err := os.MkdirAll("uploads", 0755); err != nil {
        log.Println("Error creating uploads directory:", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    // Open the file in write mode
    dst, err := os.OpenFile("uploads/"+header.Filename, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
    if err != nil {
        log.Println("Error opening file:", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy the contents of the uploaded file to the destination file
    if _, err := io.Copy(dst, file); err != nil {
        log.Println("Error copying file:", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.Write([]byte("PDF uploaded successfully"))
}