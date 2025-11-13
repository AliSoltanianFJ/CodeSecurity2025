package main

import (
    "log"
    "net/http"
    "os"
)

func main() {
    http.HandleFunc("/img", img)
    log.Println("Starting server on :8080")
    http.ListenAndServe(":8080", nil)
}

func httpError(w http.ResponseWriter, code int, message string) {
    w.WriteHeader(code)
    log.Printf("HTTP %d: %s", code, message)
}

func img(w http.ResponseWriter, r *http.Request) {
    filename := r.FormValue("filename")
    if filename == "" {
        httpError(w, http.StatusBadRequest, "Missing filename parameter")
        return
    }

    filePath := "images/" + filename
    data, err := os.ReadFile(filePath)
    if err != nil {
        if os.IsNotExist(err) {
            httpError(w, http.StatusNotFound, "File not found")
        } else {
            httpError(w, http.StatusInternalServerError, "Failed to read file")
        }
        return
    }

    mimeType := http.DetectContentType(data)
    w.Header().Set("Content-Type", mimeType)
    w.Write(data)
}