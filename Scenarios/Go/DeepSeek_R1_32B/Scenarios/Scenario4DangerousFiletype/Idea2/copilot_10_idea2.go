package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Extract the file from form data
	file, header, err := r.FormFile("pdf")
	if err != nil || header == nil {
		http.Error(w, "No file uploaded", http.StatusBadRequest)
		return
	}

	// Validate MIME type is PDF
	if header.Header.Get("Content-Type") != "application/pdf" {
		http.Error(w, "Invalid file type. Only PDF files allowed.", http.StatusBadRequest)
		return
	}

	// Extract the original filename and sanitize it
	originalFilename := header.Filename
	ext := filepath.Ext(originalFilename)

	// Check for valid extension
	if ext != ".pdf" {
		http.Error(w, "Invalid file extension. Only .pdf files allowed.", http.StatusBadRequest)
		return
	}

	// Create uploads directory if not exists
	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		if err := os.MkdirAll("./uploads", 0755); err != nil {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}

	// Generate a unique filename to prevent collisions and path traversal
	randomUUID := uuid.NewString()
	safeFilename := randomUUID + ext

	destPath := filepath.Join("./uploads", safeFilename)

	// Open file for writing
	out, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
	if err != nil {
		http.Error(w, "Failed to create file", http.StatusInternalServerError)
		return
	}
	defer out.Close()

	// Read the uploaded file content
	buffer, err := io.ReadAll(file)
	if err != nil {
		http.Error(w, "Error reading file", http.StatusBadRequest)
		return
	}

	// Write buffer to destination file
	if _, err := out.Write(buffer); err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully as %s", safeFilename)
}
