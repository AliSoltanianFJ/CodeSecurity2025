package main

import (
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
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Set a maximum upload size of 10MB
	r.ParseMultipartForm(10 << 20)

	file, handler, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error retrieving the file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate file type
	contentType := handler.Header.Get("Content-Type")
	if contentType != "application/pdf" {
		http.Error(w, "File must be a PDF", http.StatusBadRequest)
		return
	}

	// Sanitize file name
	fileName := handler.Filename
	fileName = filepath.Base(fileName)                            // Extract base name
	fileName = strings.ReplaceAll(fileName, " ", "_")            // Replace spaces with underscores
	fileName = strings.ReplaceAll(fileName, "..", "")             // Remove .. for security
	fileName = filepath.Clean(fileName)                          // Clean the path

	// Define upload directory
	uploadDir := "./uploads"
	err = os.MkdirAll(uploadDir, os.ModeDir|0755) // Create directory if not exists
	if err != nil {
		http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
		return
	}

	// Create the file path
	fpath := filepath.Join(uploadDir, fileName)

	// Create the file on disk
	dst, err := os.Create(fpath)
	if err != nil {
		http.Error(w, "Could not create file on disk", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy the uploaded file to the created file
	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Could not save the file", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully at: %s\n", fpath)
}