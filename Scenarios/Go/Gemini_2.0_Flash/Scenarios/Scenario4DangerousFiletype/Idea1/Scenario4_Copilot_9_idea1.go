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

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// 1. Retrieve the file from the request
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error retrieving the file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 2. Prepare the destination file path
	filename := filepath.Base(header.Filename) // Extract base name to prevent path injection
	uploadDir := "./uploads"
	filePath := filepath.Join(uploadDir, filename)

	// 3. Create the upload directory if it doesn't exist
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, os.ModeDir|0755); err != nil {
			http.Error(w, "Error creating upload directory", http.StatusInternalServerError)
			return
		}
	}

	// 4. Create the destination file
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Error creating the destination file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// 5. Copy the uploaded file data to the destination file
	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Error copying the file", http.StatusInternalServerError)
		return
	}

	// 6. Respond to the client
	fmt.Fprintf(w, "File uploaded successfully to: %s", filePath)
}