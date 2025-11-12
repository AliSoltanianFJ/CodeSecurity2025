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
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Error getting file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Check for PDF mime type (application/pdf)
    buffer := make([]byte, 512)
    _, err = file.Read(buffer)
    if err != nil {
        http.Error(w, "Error reading file: "+err.Error(), http.StatusBadRequest)
        return
    }
    contentType := http.DetectContentType(buffer)
    if contentType != "application/pdf" {
        http.Error(w, "Invalid file type: only PDF allowed", http.StatusBadRequest)
        return
    }
    file.Seek(0, io.SeekStart) // Reset file reader

    // Create ./uploads directory if not exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Error creating uploads directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // Sanitize filename to prevent path traversal
    filename := header.Filename
    filename = filepath.Clean(filename)
    if strings.Contains(filename, "..") {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }
    dstPath := filepath.Join(uploadDir, filename)

    // Open destination file, error if exists (prevents overwrite)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Error saving file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Copy file contents to destination
    _, err = io.Copy(dst, file)
    if err != nil {
        http.Error(w, "Error saving file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.Write([]byte("PDF uploaded successfully."))
}