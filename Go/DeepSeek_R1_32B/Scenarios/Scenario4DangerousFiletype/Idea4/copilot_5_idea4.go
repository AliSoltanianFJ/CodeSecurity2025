package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error retrieving file: %v", err)
		return
	}
	defer file.Close()

	// Validate file type is PDF
	contentType := header.Header.Get("Content-Type")
	if contentType != "application/pdf" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Invalid file type. Only PDF files are allowed.")
		return
	}

	// Maximum allowed file size (5MB in this example)
	maxFileSize := 5 * 1024 * 1024
	if header.Size > maxFileSize {
		w.WriteHeader(http.StatusRequestEntityTooLarge)
		fmt.Fprintf(w, "File too large. Max allowed: %d MB", maxFileSize/1024/1024)
		return
	}

	// Generate a unique file name to prevent overwrites and directory traversal
	fileName := fmt.Sprintf("file_%d.pdf", time.Now().UnixNano())
	destPath := filepath.Join("uploads", fileName)

	// Ensure uploads directory exists with proper permissions
	if err := os.MkdirAll("uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Failed to create uploads directory: %v", err)
		return
	}

	// Open or create the destination file with appropriate permissions
	destFile, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Failed to open destination file: %v", err)
		return
	}
	defer destFile.Close()

	// Copy the uploaded file contents to the destination file
	if _, err := io.Copy(destFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error saving file: %v", err)
		return
	}

	fmt.Fprintf(w, "File saved successfully as: %s", fileName)
}
