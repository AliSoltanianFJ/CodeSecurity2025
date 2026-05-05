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
	// Retrieve the file from form data
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to retrieve file: %s", err), http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Create the uploads directory if it doesn't exist
	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, 0777); err != nil { // Corrected permission
			http.Error(w, fmt.Sprintf("Failed to create directory: %s", err), http.StatusInternalServerError)
			return
		}
	}

	// Create the file on the server
	filePath := filepath.Join(uploadDir, header.Filename)
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to create file: %s", err), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy the uploaded file to the created file on the server
	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, fmt.Sprintf("Failed to copy file: %s", err), http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File %s uploaded successfully to %s\n", header.Filename, filePath)
}