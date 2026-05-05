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
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Sanitize filename (only allow alphanumeric, underscores, hyphens, and dots)
    filename := header.Filename
    sanitizedName := strings.Map(func(r rune) rune {
        if r == ' ' || r == '/' || r == '\\' {
            return '_'
        }
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '_' || r == '-' || r == '.' {
            return r
        }
        return '_'
    }, filename)

    // Check if filename is empty after sanitization
    if sanitizedName == "" {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Check if file extension is .pdf (case-insensitive)
    if !strings.EqualFold(filepath.Ext(sanitizedName), ".pdf") {
        http.Error(w, "Only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists
    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Create a unique file path using uuid
    filePath := filepath.Join(uploadsDir, fmt.Sprintf("%s_%s", strings.TrimSuffix(sanitizedName, ".pdf"), uuid.New().String()+".pdf"))

    // Open the file for writing
    outputFile, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer outputFile.Close()

    // Copy file content, limiting the size to 10MB
    written, err := io.Copy(outputFile, io.LimitReader(file, 10<<20))
    if err != nil {
        os.Remove(filePath)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure at least one byte was written
    if written == 0 {
        os.Remove(filePath)
        http.Error(w, "Empty file", http.StatusBadRequest)
        return
    }

    fmt.Fprintf(w, "File uploaded successfully to: %s\n", filePath)
}