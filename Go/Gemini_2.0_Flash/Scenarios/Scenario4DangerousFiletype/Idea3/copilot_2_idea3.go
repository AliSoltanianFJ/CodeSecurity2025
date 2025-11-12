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

    // Parse the form with a limit of 32 MB
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Get the uploaded file from the form
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Missing or invalid file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate file type (must be PDF)
    mimeType := header.Header.Get("Content-Type")
    if mimeType != "application/pdf" {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Create the uploads directory if it doesn't exist
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Failed to create upload directory", http.StatusInternalServerError)
        return
    }

    // Sanitize filename (only allow alphanumeric, dots, and dashes)
    fileName := header.Filename
    safeFileName := strings.Map(func(r rune) rune {
        if r >= 'a' && r <= 'z' || r >= 'A' && r <= 'Z' || r >= '0' && r <= '9' || r == '.' || r == '-' {
            return r
        }
        return '_'
    }, fileName)

    // Ensure no path traversal
    if strings.Contains(safeFileName, "..") || strings.HasPrefix(safeFileName, "/") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Create and open the destination file in ./uploads
    dstPath := filepath.Join(uploadDir, safeFileName)
    dstFile, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    defer dstFile.Close()

    // Copy the file content to the destination file
    written, err := io.Copy(dstFile, file)
    if err != nil {
        http.Error(w, "Failed to copy file", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "File '%s' uploaded successfully (%d bytes)", safeFileName, written)
}