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
    // Only allow POST requests
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Ensure the uploads directory exists
    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Parse form and get file
    err := r.ParseMultipartForm(10 << 20) // 10 MB max upload
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "No file uploaded", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Only allow .pdf extension
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only PDF files allowed", http.StatusBadRequest)
        return
    }

    // Sanitize filename to prevent path traversal
    filename := header.Filename
    filename = filepath.Clean(filename)
    if strings.Contains(filename, "..") || strings.HasPrefix(filename, "/") {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Write file to uploads directory
    dstPath := filepath.Join(uploadsDir, filename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    _, err = io.Copy(dst, file)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}