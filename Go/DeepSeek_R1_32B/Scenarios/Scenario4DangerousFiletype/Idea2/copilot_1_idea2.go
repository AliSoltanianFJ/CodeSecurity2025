package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gocolly/colly/v2"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	// Parse the multipart form and handle errors
	err := r.ParseMultipartForm(10 << 20) // Set max memory to 10MB
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error parsing form: %v", err)
		return
	}

	// Check if the file was uploaded and has content type application/pdf
	file, header, err := r.FormFile("pdf")
	if err != nil || file == nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "No PDF file uploaded or invalid file")
		return
	}

	contentType := header.Header.Get("Content-Type")
	if contentType != "application/pdf" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Invalid file type. Only PDF files are allowed.")
		return
	}

	// Sanitize the filename to prevent path traversal attacks
	filename := sanitizeFilename(header.Filename)

	// Ensure the uploads directory exists
	if err := os.MkdirAll("uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating upload directory: %v", err)
		return
	}

	// Save file to disk with restricted permissions
	destPath := filepath.Join("uploads", filename)
	f, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error saving file: %v", err)
		return
	}
	defer f.Close()

	// Copy the uploaded file to disk safely
	if _, err := io.Copy(f, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error writing file: %v", err)
		return
	}

	// Return success response
	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "File uploaded successfully")
}

// Sanitize the filename to prevent path traversal attacks
func sanitizeFilename(name string) string {
	sanitized := colly.SanitizeFileName(name)
	if sanitized == "" {
		return "untitled.pdf"
	}
	return sanitized
}
