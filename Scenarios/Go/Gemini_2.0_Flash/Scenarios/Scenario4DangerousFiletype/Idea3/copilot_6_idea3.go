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
        http.Error(w, "Invalid file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Sanitize filename
    filename := filepath.Base(header.Filename)
    ext := strings.ToLower(filepath.Ext(filename))
    if ext != ".pdf" {
        http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
        return
    }
    // Generate a unique name to avoid clashes (optional)
    dstFilename := filepath.Join("uploads", fmt.Sprintf("%d_%s", time.Now().UnixNano(), filename))
    dst, err := os.OpenFile(dstFilename, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // Limit the file size to 10MB (adjust as needed)
    limitedReader := io.LimitReader(file, 10<<20)
    if _, err = io.Copy(dst, limitedReader); err != nil {
        os.Remove(dstFilename) // Cleanup if copy fails
        http.Error(w, "File too large or write error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "File uploaded successfully: %s\n", dstFilename)
}