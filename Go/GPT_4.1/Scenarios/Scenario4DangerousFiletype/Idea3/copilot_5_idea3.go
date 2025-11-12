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
        http.Error(w, "Error reading file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "Unable to create upload directory", http.StatusInternalServerError)
        return
    }

    // Sanitize filename: get only the base name and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }

    // Remove potentially dangerous characters
    filename = strings.Map(func(r rune) rune {
        if r == '.' || r == '_' || r == '-' || (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') {
            return r
        }
        return -1
    }, filename)

    if filename == "" {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Prevent file overwrite by adding a timestamp
    dstPath := filepath.Join(uploadDir, time.Now().Format("20060102_150405_")+filename)

    out, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Unable to save file", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    // Limit file size to 10MB
    const maxFileSize = 10 << 20 // 10 MB
    limitedReader := io.LimitReader(file, maxFileSize)
    if _, err := io.Copy(out, limitedReader); err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }

    w.Write([]byte("File uploaded successfully"))
}