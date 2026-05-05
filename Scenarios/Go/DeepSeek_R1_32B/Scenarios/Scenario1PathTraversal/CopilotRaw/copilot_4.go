package main

import (
    "fmt"
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
    if filename == "" {
        http.Error(w, "No filename provided", http.StatusBadRequest)
        return
    }

    filePath := fmt.Sprintf("images/%s", filename)
    data, err := os.ReadFile(filePath)
    if err != nil {
        if os.IsNotExist(err) {
            http.Error(w, "File not found", http.StatusNotFound)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }

    contentType := http.DetectContentType(data)
    w.Header().Set("Content-Type", contentType)
    io.Copy(w, data)
}