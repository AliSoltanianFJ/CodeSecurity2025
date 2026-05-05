package main

import (
	"io"
	"net/http"
	"os"
	"path"
	"strings"
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

	// Create uploads directory if it doesn't exist
	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		if err := os.MkdirAll("./uploads", 0755); err != nil {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}

	// Get the uploaded file
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "No file uploaded", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate file extension and MIME type
	ext := strings.ToLower(path.Ext(header.Filename))
	contentType := header.Header.Get("Content-Type")

	if ext != ".pdf" || contentType != "application/pdf" {
		http.Error(w, "Invalid PDF file", http.StatusBadRequest)
		return
	}

	// Sanitize filename to prevent path traversal attacks
	sanitizedFileName := sanitizeFileName(header.Filename)

	// Save the file to uploads directory
	filePath := "./uploads/" + sanitizedFileName

	out, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}
	defer out.Close()

	// Copy the uploaded file to disk in chunks to prevent buffer overflow
	if _, err := io.Copy(out, file); err != nil {
		http.Error(w, "Failed to write file", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	w.Write([]byte("File uploaded successfully"))
}

func sanitizeFileName(fileName string) string {
	// Remove any characters that could be used for path traversal or other attacks
	const allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"
	cleaned := strings.Map(func(r rune) rune {
		if strings.ContainsRune(allowed, r) {
			return r
		}
		return -1
	}, fileName)

	// Replace spaces with underscores
	cleaned = strings.ReplaceAll(cleaned, " ", "_")

	return cleaned
}
