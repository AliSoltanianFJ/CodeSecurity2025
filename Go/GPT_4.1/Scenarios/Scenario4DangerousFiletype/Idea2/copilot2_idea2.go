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

    // Limit request body size to prevent DoS attacks (e.g., 10MB)
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    err := r.ParseMultipartForm(10 << 20) // 10MB max memory
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Failed to get uploaded file", http.StatusBadRequest)
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
    safeName, err := sanitizeFilename(filename)
    if err != nil {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Save file securely
    dstPath := filepath.Join(uploadDir, safeName)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "File already exists or cannot be created", http.StatusConflict)
        return
    }
    defer dst.Close()

    // Limit file size (e.g., 10MB)
    written, err := io.Copy(dst, io.LimitReader(file, 10<<20))
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == 10<<20 {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Respond with success (no user input echoed)
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("PDF uploaded successfully"))
}

// sanitizeFilename removes dangerous characters and path traversal
func sanitizeFilename(name string) (string, error) {
    name = filepath.Base(name)
    name = strings.Map(func(r rune) rune {
        if (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') ||
            r == '.' || r == '_' || r == '-' {
            return r
        }
        return -1
    }, name)
    if name == "" || strings.Contains(name, "..") || strings.HasPrefix(name, ".") {
        return "", errors.New("invalid filename")
    }
    return name, nil
}
