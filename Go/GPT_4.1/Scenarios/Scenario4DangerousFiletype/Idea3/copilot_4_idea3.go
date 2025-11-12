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

    // Sanitize filename: get only the base name and ensure .pdf extension
    filename := filepath.Base(header.Filename)
    if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }
    // Remove any non-alphanumeric, dash, underscore, or dot characters from filename
    filename = sanitizeFilename(filename)

    dstPath := filepath.Join(uploadDir, filename)

    // Prevent file overwrite
    out, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if errors.Is(err, os.ErrExist) {
        http.Error(w, "File already exists", http.StatusConflict)
        return
    }
    if err != nil {
        http.Error(w, "Unable to save file", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    // Limit file size to 10 MB
    written, err := io.Copy(out, io.LimitReader(file, 10<<20))
    if err != nil {
        http.Error(w, "Error saving file", http.StatusInternalServerError)
        return
    }
    if written == 10<<20 {
        http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("File uploaded successfully"))
}

// sanitizeFilename removes dangerous characters from filenames
func sanitizeFilename(name string) string {
    // Allow only alphanumeric, dash, underscore, and dot
    re := regexp.MustCompile(`[^a-zA-Z0-9._-]`)
    return re.ReplaceAllString(name, "_")
}