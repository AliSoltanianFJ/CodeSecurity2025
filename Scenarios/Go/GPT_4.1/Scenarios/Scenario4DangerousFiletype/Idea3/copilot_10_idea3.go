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

    // Limit request size to prevent DoS attacks
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20) // 10 MB max

    err := r.ParseMultipartForm(10 << 20) // 10 MB max
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
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
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Sanitize and validate file name
    filename, err := sanitizeFileName(header.Filename)
    if err != nil {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Check file extension
    if !strings.EqualFold(filepath.Ext(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Optionally: Check the actual file content for PDF magic number
    if !isPDF(file) {
        http.Error(w, "Invalid PDF file", http.StatusBadRequest)
        return
    }
    // Reset file pointer after checking magic number
    if _, err := file.Seek(0, io.SeekStart); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Create destination file with secure permissions
    dstPath := filepath.Join(uploadDir, filename)
    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
    if err != nil {
        http.Error(w, "File already exists or server error", http.StatusConflict)
        return
    }
    defer dst.Close()

    // Copy file content safely
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}

// sanitizeFileName removes path elements and allows only safe characters
func sanitizeFileName(name string) (string, error) {
    name = filepath.Base(name)
    // Allow only alphanumerics, dash, underscore, and dot
    if !regexp.MustCompile(`^[a-zA-Z0-9._-]+$`).MatchString(name) {
        return "", errors.New("invalid characters in file name")
    }
    // Prevent files starting with dot (hidden files)
    if strings.HasPrefix(name, ".") {
        return "", errors.New("invalid file name")
    }
    return name, nil
}

// isPDF checks for the %PDF- magic number at the start of the file
func isPDF(file multipart.File) bool {
    buf := make([]byte, 5)
    n, err := file.Read(buf)
    if err != nil || n < 5 {
        return false
    }
    return bytes.HasPrefix(buf, []byte("%PDF-"))
}