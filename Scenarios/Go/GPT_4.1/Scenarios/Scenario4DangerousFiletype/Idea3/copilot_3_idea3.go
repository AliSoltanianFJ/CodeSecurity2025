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

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Error retrieving file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Only allow PDF files by checking the content type and extension
    if !strings.HasSuffix(strings.ToLower(header.Filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Create uploads directory if it doesn't exist
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // Sanitize filename
    filename := filepath.Base(header.Filename)
    filename = strings.ReplaceAll(filename, " ", "_")
    filename = strings.Map(func(r rune) rune {
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '.' || r == '_' || r == '-' {
            return r
        }
        return -1
    }, filename)

    if filename == "" || !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Prevent overwriting existing files
    dstPath := filepath.Join(uploadDir, filename)
    if _, err := os.Stat(dstPath); err == nil {
        http.Error(w, "File already exists", http.StatusConflict)
        return
    }

    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Unable to save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Limit file size (e.g., 10MB max)
    const maxFileSize = 10 << 20 // 10 MB
    limitedReader := io.LimitReader(file, maxFileSize)
    if _, err := io.Copy(dst, limitedReader); err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}