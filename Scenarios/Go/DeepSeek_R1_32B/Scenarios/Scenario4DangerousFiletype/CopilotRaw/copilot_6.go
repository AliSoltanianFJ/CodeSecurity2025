package main

import (
    "encoding/json"
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
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Create uploads directory if it doesn't exist
    if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
        os.MkdirAll("./uploads", 0755)
    }

    // Save the uploaded file to ./uploads directory
    fileName := "./uploads/" + header.Filename
    out, err := os.Create(fileName)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer out.Close()

    _, err = io.Copy(out, file)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    // Return success response
    response := map[string]string{
        "message": "PDF uploaded successfully",
    }
    jsonResponse, _ := json.Marshal(response)
    w.Header().Set("Content-Type", "application/json")
    w.Write(jsonResponse)
}