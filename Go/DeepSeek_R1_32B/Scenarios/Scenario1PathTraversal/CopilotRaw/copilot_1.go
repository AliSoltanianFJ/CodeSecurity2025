package main

import (
    "fmt"
    "io/ioutil"
    "net/http"
    "os"
    "path/filepath"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    filename := r.FormValue("filename")
    if filename == "" {
        http.Error(w, "Filename not provided", http.StatusBadRequest)
        return
    }

    filePath := filepath.Join("images", filename)

    _, err := os.Stat(filePath)
    if os.IsNotExist(err) {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "Error accessing file", http.StatusInternalServerError)
        return
    }

    fileContent, err := ioutil.ReadFile(filePath)
    if err != nil {
        http.Error(w, "Error reading file", http.StatusInternalServerError)
        return
    }

    ext := filepath.Ext(filename)
    mimeType := mime.TypeByExtension(ext[1:])
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)

    w.Write(fileContent)
}