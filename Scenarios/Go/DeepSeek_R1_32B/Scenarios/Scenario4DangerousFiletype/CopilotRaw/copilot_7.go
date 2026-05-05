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
        io.WriteString(w, "Error retrieving file from form: "+err.Error())
        return
    }
    defer file.Close()

    if !fileAvailable {
        os.MkdirAll("./uploads", os.ModePerm)
    }

    dst, err := os.Create(filepath.Join("./uploads", filepath.Base(header.Filename)))
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        io.WriteString(w, "Error creating file: "+err.Error())
        return
    }
    defer dst.Close()

    if _, err = io.Copy(dst, file); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        io.WriteString(w, "Error copying file: "+err.Error())
        return
    }

    w.WriteHeader(http.StatusOK)
    io.WriteString(w, "File uploaded successfully")
}