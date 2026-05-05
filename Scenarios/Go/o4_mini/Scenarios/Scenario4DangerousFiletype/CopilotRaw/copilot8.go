// ...existing code...
package main

import (
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit memory used for parsing multipart form
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "failed to parse multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // ensure uploads directory exists
    dstDir := "./uploads"
    if err := os.MkdirAll(dstDir, 0755); err != nil {
        http.Error(w, "failed to create uploads directory", http.StatusInternalServerError)
        return
    }

    // sanitize filename and enforce .pdf extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "only PDF files are allowed", http.StatusBadRequest)
        return
    }

    dstPath := filepath.Join(dstDir, filename)

    out, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    if _, err := io.Copy(out, file); err != nil {
        http.Error(w, "failed to save uploaded file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("file uploaded"))
}
// ...existing code...