package main

import (
    "io"
    "log"
    "os"
    "path/filepath"
    "net/http"
)

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != "POST" {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        log.Printf("Error retrieving file: %v", err)
        http.Error(w, "Failed to retrieve the file", http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Create uploads directory if it doesn't exist
    if err := os.MkdirAll("./uploads", 0755); err != nil {
        log.Printf("Error creating uploads directory: %v", err)
        http.Error(w, "Failed to create uploads directory", http.StatusInternalServerError)
        return
    }

    // Create path for the file
    fileName := header.Filename
    filePath := filepath.Join("./uploads", fileName)

    // Open the file for writing
    outFile, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE, 0644)
    if err != nil {
        log.Printf("Error opening output file: %v", err)
        http.Error(w, "Failed to open output file", http.StatusInternalServerError)
        return
    }
    defer outFile.Close()

    // Copy the contents of the uploaded file to the output file
    if _, err := io.Copy(outFile, file); err != nil {
        log.Printf("Error writing file: %v", err)
        http.Error(w, "Failed to save the file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("File uploaded successfully"))
}