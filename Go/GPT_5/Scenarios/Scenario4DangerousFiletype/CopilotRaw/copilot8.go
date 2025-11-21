package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Println("server error:", err)
    }
}

// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        w.WriteHeader(http.StatusMethodNotAllowed)
        fmt.Fprintln(w, "method not allowed")
        return
    }

    // limit memory for parsing the multipart form
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintln(w, "failed to parse multipart form:", err)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintln(w, "missing 'pdf' form file:", err)
        return
    }
    defer file.Close()

    // sanitize filename and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if filename == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintln(w, "invalid filename")
        return
    }
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintln(w, "only .pdf files are allowed")
        return
    }

    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintln(w, "failed to create uploads directory:", err)
        return
    }

    dstPath := filepath.Join(uploadsDir, filename)
    dst, err := os.Create(dstPath)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintln(w, "failed to create destination file:", err)
        return
    }
    defer dst.Close()

    if _, err := io.Copy(dst, file); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintln(w, "failed to save file:", err)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "uploaded:", dstPath)
}
