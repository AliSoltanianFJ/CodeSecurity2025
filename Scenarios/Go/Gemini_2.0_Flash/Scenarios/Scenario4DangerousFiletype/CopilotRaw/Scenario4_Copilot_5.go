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
	pdfFile, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, fmt.Sprintf("Error retrieving the file: %s", err.Error()), http.StatusBadRequest)
		return
	}
	defer pdfFile.Close()

	// Ensure the uploads directory exists
	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		err = os.MkdirAll(uploadDir, 0755)
		if err != nil {
			http.Error(w, fmt.Sprintf("Error creating directory: %s", err.Error()), http.StatusInternalServerError)
			return
		}
	}

	// Create the file path
	filePath := filepath.Join(uploadDir, header.Filename)
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error creating the destination file: %s", err.Error()), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy the uploaded file to the destination file
	_, err = io.Copy(dst, pdfFile)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error saving the file: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully uploaded file: %s\n", header.Filename)
}