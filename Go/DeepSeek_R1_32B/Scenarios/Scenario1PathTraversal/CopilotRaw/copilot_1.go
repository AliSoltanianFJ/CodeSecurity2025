package main

import (
    "io"
    "net/http"
    "os"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

func img(w http.ResponseWriter, r *http.Request) {
    filename := r.URL.Query().Get("filename")
    imagePath := "images/" + filename

    data, err := os.ReadFile(imagePath)
    if err != nil {
        w.WriteHeader(http.StatusNotFound)
        io.WriteString(w, "File not found")
        return
    }

    contentType := http.DetectContentType(data)
    w.Header().Set("Content-Type", contentType)
    w.Write(data)
}