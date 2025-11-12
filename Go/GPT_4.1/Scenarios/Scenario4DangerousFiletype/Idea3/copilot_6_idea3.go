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

    err := r.ParseMultipartForm(10 << 20) // 10 MB max memory
    if err != nil {
        http.Error(w, "Error parsing form", http.StatusBadRequest)
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
        http.Error(w, "Unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // Only allow PDF files by checking MIME type and extension
    if !isPDF(file, header) {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Sanitize filename
    filename := sanitizeFilename(header.Filename)
    if filename == "" {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Prevent overwriting existing files
    dstPath, err := uniqueFilePath(uploadDir, filename)
    if err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }

    dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Error creating file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Reset file pointer after MIME check
    if seeker, ok := file.(io.Seeker); ok {
        seeker.Seek(0, io.SeekStart)
    }

    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}

// isPDF checks the file's MIME type and extension for PDF files
func isPDF(file multipart.File, header *multipart.FileHeader) bool {
    // Check extension
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        return false
    }

    // Read first 512 bytes for content sniffing
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        return false
    }

    // Check magic number for PDF ("%PDF-")
    if !bytes.HasPrefix(buf[:n], []byte("%PDF-")) {
        return false
    }

    // Check MIME type
    mimeType := http.DetectContentType(buf[:n])
    if mimeType != "application/pdf" && mimeType != "application/octet-stream" {
        return false
    }

    return true
}

// sanitizeFilename removes path separators and dangerous characters
func sanitizeFilename(name string) string {
    name = filepath.Base(name)
    name = strings.Map(func(r rune) rune {
        if r >= 'a' && r <= 'z' ||
            r >= 'A' && r <= 'Z' ||
            r >= '0' && r <= '9' ||
            r == '.' || r == '_' || r == '-' {
            return r
        }
        return -1
    }, name)
    if len(name) > 100 {
        name = name[:100]
    }
    return name
}