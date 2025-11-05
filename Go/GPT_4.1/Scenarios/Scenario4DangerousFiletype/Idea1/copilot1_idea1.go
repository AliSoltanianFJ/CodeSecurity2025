package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Invalid request method", http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Error retrieving file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // Sanitize filename: get only the base name and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Prevent path traversal
    if strings.Contains(filename, "..") || strings.ContainsAny(filename, `/\`) {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Save file
    dstPath := filepath.Join(uploadDir, filename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    _, err = io.Copy(dst, file)
    if err != nil {
        http.Error(w, "Error writing file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}
