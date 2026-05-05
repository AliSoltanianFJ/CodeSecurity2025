package main

import (
    "errors"
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

    // Limit request body size to prevent DoS (e.g., 10MB)
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    err := r.ParseMultipartForm(10 << 20) // 10MB
    if err != nil {
        http.Error(w, "File too large or invalid form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate file extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Sanitize filename (remove path, allow only safe chars)
    safeName := sanitizeFilename(filename)
    if safeName == "" {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Check PDF magic number (first 4 bytes should be "%PDF")
    buf := make([]byte, 4)
    _, err = io.ReadFull(file, buf)
    if err != nil || string(buf) != "%PDF" {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }

    // Reset file pointer to start
    _, err = file.Seek(0, io.SeekStart)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadDir, safeName)

    // Prevent overwriting existing files
    if _, err := os.Stat(dstPath); err == nil {
        http.Error(w, "File already exists", http.StatusConflict)
        return
    }

    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Could not save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy file with size limit (10MB)
    written, err := io.CopyN(dst, file, 10<<20)
    if err != nil && !errors.Is(err, io.EOF) {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == 10<<20 {
        http.Error(w, "File too large", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte("File uploaded successfully"))
}

// sanitizeFilename removes dangerous characters and returns a safe filename
func sanitizeFilename(name string) string {
    name = filepath.Base(name)
    name = strings.Map(func(r rune) rune {
        if (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') ||
            r == '.' || r == '-' || r == '_' {
            return r
        }
        return '_'
    }, name)
    if len(name) > 100 {
        name = name[:100]
    }
    return name
}
