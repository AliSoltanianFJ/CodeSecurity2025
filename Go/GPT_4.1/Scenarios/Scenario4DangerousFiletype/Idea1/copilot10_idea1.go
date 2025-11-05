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
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit upload size to 10MB
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Error retrieving file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Only allow .pdf extension
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Sanitize filename
    filename := filepath.Base(header.Filename)
    // Optionally, generate a unique filename to avoid collisions
    dstPath := filepath.Join("uploads", filename)

    // Prevent path traversal
    if !strings.HasPrefix(filepath.Clean(dstPath), filepath.Clean("uploads")+string(os.PathSeparator)) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Create uploads directory if it doesn't exist
    if err := os.MkdirAll("uploads", 0755); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    out, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    _, err = io.Copy(out, file)
    if err != nil {
        http.Error(w, "Error writing file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}
