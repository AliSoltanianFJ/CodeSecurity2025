package main

import (
	"net/http"
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

    // Limit request body to prevent DOS (e.g., 10MB)
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    err := r.ParseMultipartForm(10 << 20) // 10MB
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Failed to get file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Check file extension (only allow .pdf)
    if !strings.EqualFold(filepath.Ext(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Sanitize filename: get only the base, remove dangerous chars, and generate random name
    safeName, err := generateSafeFilename(".pdf")
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadDir, safeName)

    // Open destination file with secure permissions
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy file contents safely (limit size)
    written, err := io.Copy(dst, io.LimitReader(file, 10<<20)) // 10MB max
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    if written == 10<<20 {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}

// generateSafeFilename creates a random filename with the given extension
func generateSafeFilename(ext string) (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b) + ext, nil
}