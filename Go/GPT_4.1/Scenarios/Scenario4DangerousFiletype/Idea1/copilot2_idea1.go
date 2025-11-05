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

    err := r.ParseMultipartForm(10 << 20) // 10 MB max memory
    if err != nil {
        http.Error(w, "Could not parse multipart form", http.StatusBadRequest)
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
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    // Sanitize filename: get base name and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Prevent path traversal
    if filename == "." || filename == ".." || strings.Contains(filename, string(os.PathSeparator)) {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Optionally, generate a unique filename to avoid overwrites
    dstPath := filepath.Join(uploadDir, filename)
    out, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    // Optionally, check file header for PDF magic number
    buf := make([]byte, 5)
    n, err := file.Read(buf)
    if err != nil || n < 5 || string(buf) != "%PDF-" {
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }
    // Write the header and the rest of the file
    if _, err := out.Write(buf); err != nil {
        http.Error(w, "Error writing file", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(out, file); err != nil {
        http.Error(w, "Error writing file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}
