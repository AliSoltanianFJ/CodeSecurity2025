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
    if r.Method != "POST" {
        w.WriteHeader(http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer file.Close()

    if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
        os.Mkdir("./uploads", 0755)
    }

    outfile, err := os.Create("./uploads/" + header.Filename)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer outfile.Close()

    if _, err := io.Copy(outfile, file); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    io.WriteString(w, "OK")
}