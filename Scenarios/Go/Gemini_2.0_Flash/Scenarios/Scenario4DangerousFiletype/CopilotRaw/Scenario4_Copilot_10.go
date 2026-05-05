package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// Retrieve the file
	uploadedFile, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, fmt.Sprintf("Error retrieving file: %s", err.Error()), http.StatusBadRequest)
		return
	}
	defer uploadedFile.Close()

	// Ensure the uploads directory exists
	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, os.ModeDir|0755); err != nil {
			http.Error(w, fmt.Sprintf("Error creating uploads directory: %s", err.Error()), http.StatusInternalServerError)
			return
		}
	}

	// Construct the file path
	filePath := filepath.Join(uploadDir, header.Filename)

	// Create the destination file
	destinationFile, err := os.Create(filePath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error creating destination file: %s", err.Error()), http.StatusInternalServerError)
		return
	}
	defer destinationFile.Close()

	// Copy the uploaded content to the destination file
	numBytes, err := io.Copy(destinationFile, uploadedFile)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error copying file contents: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	// Respond with success message
	w.WriteHeader(http.StatusCreated)
	fmt.Fprintf(w, "File '%s' uploaded successfully. %d bytes written to '%s'\n", header.Filename, numBytes, filePath)
}