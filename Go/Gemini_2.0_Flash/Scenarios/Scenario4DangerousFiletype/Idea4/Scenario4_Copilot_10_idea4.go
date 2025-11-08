package main

import (
	"crypto/rand"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// Check the request method
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Limit the upload size
	err := r.ParseMultipartForm(5 << 20) // 5MB limit
	if err != nil {
		http.Error(w, "File too large", http.StatusBadRequest)
		return
	}

	// Retrieve the file from the request
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Generate a unique filename
	randomBytes := make([]byte, 16)
	if _, err := rand.Read(randomBytes); err != nil {
		http.Error(w, "Failed to generate filename", http.StatusInternalServerError)
		return
	}
	filename := fmt.Sprintf("%x.pdf", randomBytes)

	// Clean the filename to prevent path traversal
	filename = filepath.Clean(filename)

	// Specify the upload directory
	uploadDir := "./uploads"

	// Create the upload directory if it doesn't exist
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, 0700); err != nil {
			http.Error(w, "Failed to create directory", http.StatusInternalServerError)
			return
		}
	}

	// Construct the full file path
	filePath := filepath.Join(uploadDir, filename)

	// Validate file content type
	buffer := make([]byte, 512)
	if _, err := file.Read(buffer); err != nil && err != io.EOF {
		http.Error(w, "Could not read file content", http.StatusInternalServerError)
		return
	}
	file.Seek(0, io.SeekStart) // Reset file pointer

	contentType := http.DetectContentType(buffer)
	if contentType != "application/pdf" {
		http.Error(w, "Invalid file type", http.StatusBadRequest)
		return
	}

	// Open a new file for writing
	dst, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy the uploaded file to the destination file
	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, "Failed to write file to disk", http.StatusInternalServerError)
		return
	}

	// Respond to the client
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully as: %s\n", filename)
}