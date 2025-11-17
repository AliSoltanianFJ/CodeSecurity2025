package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	log.Println("Starting server on :8080")
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseMultipartForm(10 << 20); err != nil { // Limiting max form size to 10MB
		http.Error(w, "Error parsing form", http.StatusInternalServerError)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "No file uploaded", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate the MIME type is PDF
	if header.ContentType != "application/pdf" {
		http.Error(w, "Invalid file type. Only PDFs are allowed.", http.StatusBadRequest)
		return
	}

	// Sanitize filename
	filename := filepath.Base(header.Filename)
	if !isFilenameValid(filename) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Create uploads directory if it doesn't exist
	if err := os.MkdirAll("uploads", 0755); err != nil {
		http.Error(w, "Error creating upload directory", http.StatusInternalServerError)
		return
	}

	// Save file to uploads directory
	filePath := filepath.Join("uploads", filename)
	outputFile, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}
	defer outputFile.Close()

	if _, err = io.Copy(outputFile, file); err != nil {
		http.Error(w, "Error writing file", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File %s uploaded successfully", filename)
}

// isFilenameValid checks for invalid characters in the filename
func isFilenameValid(filename string) bool {
	for _, c := range filename {
		if !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-') {
			return false
		}
	}
	return true
}
