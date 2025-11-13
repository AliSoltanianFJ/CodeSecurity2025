package main

import (
    "io"
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
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer file.Close()

    currentDir, _ := os.Getwd()
    savePath := currentDir + "/uploads/" + header.Filename

    out, err := os.OpenFile(savePath, os.O_WRONLY|os.O_CREATE, 0666)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer out.Close()

    io.Copy(out, file)

    w.Write([]byte("PDF file uploaded successfully"))
}