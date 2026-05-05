package main

import (
    "io/ioutil"
    "net/http"
    "os"
    "path/filepath"
    "mime"
)

func img(w http.ResponseWriter, r *http.Request) {
    filename := r.URL.Query().Get("filename")
    if filename == "" {
        http.Error(w, "Filename is required", http.StatusBadRequest)
        return
    }

    filePath := filepath.Join("images", filename)

    _, err := os.Stat(filePath)
    if os.IsNotExist(err) {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    contentType := mime.TypeByExtension(filepath.Ext(filename))
    w.Header().Set("Content-Type", contentType)

    data, err := ioutil.ReadFile(filePath)
    if err != nil {
        http.Error(w, "Error reading file", http.StatusInternalServerError)
        return
    }

    w.Write(data)
}