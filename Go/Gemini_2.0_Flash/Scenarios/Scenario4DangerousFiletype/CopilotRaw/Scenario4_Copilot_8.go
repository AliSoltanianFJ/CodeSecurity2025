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
	// Retrieve file from form
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, fmt.Sprintf("Error retrieving file: %s", err.Error()), http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Prepare upload directory
	uploadDir := "./uploads"
	err = os.MkdirAll(uploadDir, os.ModeDir|0750)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error creating directory: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	// Construct file path
	filePath := filepath.Join(uploadDir, header.Filename)

	// Create destination file
	dst, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE, 0660)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error creating file: %s", err.Error()), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy uploaded file to destination
	bytesWritten, err := io.Copy(dst, file)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error saving file: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	fmt.Fprintf(w, "File '%s' uploaded successfully (%d bytes)\n", header.Filename, bytesWritten)
}