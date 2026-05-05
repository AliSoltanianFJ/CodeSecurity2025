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

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Error creating upload directory", http.StatusInternalServerError)
        return
    }

    // Sanitize filename: only allow alphanumerics, dash, underscore, and .pdf extension
    filename := filepath.Base(header.Filename)
    if !isValidPDFName(filename) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    dstPath := filepath.Join(uploadDir, filename)

    // Prevent overwriting existing files
    out, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "File already exists or cannot be created", http.StatusConflict)
        return
    }
    defer out.Close()

    // Limit file size (e.g., 10MB max)
    const maxFileSize = 10 << 20 // 10 MB
    limitedReader := io.LimitReader(file, maxFileSize)
    written, err := io.Copy(out, limitedReader)
    if err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }

    // Optionally, check PDF magic number (simple check)
    if !isPDF(dstPath) {
        os.Remove(dstPath)
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}

// Only allow filenames matching: letters, numbers, underscore, dash, and ending with .pdf
func isValidPDFName(name string) bool {
    allowed := regexp.MustCompile(`^[a-zA-Z0-9_\-]+\.pdf$`)
    return allowed.MatchString(name)
}

// Simple PDF magic number check
func isPDF(path string) bool {
    f, err := os.Open(path)
    if err != nil {
        return false
    }
    defer f.Close()
    buf := make([]byte, 5)
    n, err := f.Read(buf)
    if err != nil || n < 5 {
        return false
    }
    return string(buf) == "%PDF-"
}