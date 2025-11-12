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
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse the multipart form
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    // Get the "pdf" file from the form
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "Missing or invalid pdf field", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Validate file extension as .pdf (case-insensitive)
    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "Only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // Clean the filename for security (avoid path traversal)
    safeFilename := filepath.Base(filepath.Clean(filepath.Abs(header.Filename)))
    outputPath := filepath.Join("uploads", safeFilename)

    // Create the "uploads" directory if it doesn't exist
    if err := os.MkdirAll("uploads", 0755); err != nil {
        http.Error(w, "Failed to create uploads directory", http.StatusInternalServerError)
        return
    }

    // Open the output file in exclusive mode (avoid overwriting)
    outFile, err := os.OpenFile(outputPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }
    defer outFile.Close()

    // Copy the file content
    _, err = io.Copy(outFile, file)
    if err != nil {
        http.Error(w, "Failed to save file", http.StatusInternalServerError)
        return
    }

    // Response with a success message
    fmt.Fprintf(w, "PDF uploaded successfully as %s", safeFilename)
}