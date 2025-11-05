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

    // Only allow .pdf extension (case-insensitive)
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Sanitize filename: remove path, allow only safe characters
    filename := filepath.Base(header.Filename)
    filename = strings.Map(func(r rune) rune {
        if (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') ||
            r == '.' || r == '_' || r == '-' {
            return r
        }
        return '_'
    }, filename)

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
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

    // Optionally, check file header for PDF magic number
    buf := make([]byte, 4)
    n, err := file.Read(buf)
    if err != nil || n < 4 || string(buf) != "%PDF" {
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }
    if _, err := dst.Write(buf); err != nil {
        http.Error(w, "Error writing file", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Error writing file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}
