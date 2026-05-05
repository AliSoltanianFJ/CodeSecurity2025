package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
)

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error retrieving file: %v", err)
		return
	}
	defer file.Close()

	// Sanitize the filename to prevent path traversal attacks
	filename := filepath.Base(header.Filename)

	// Ensure uploads directory exists with proper permissions
	if err := os.MkdirAll("./uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating upload directory: %v", err)
		return
	}

	// Create the destination file path
	dstPath := filepath.Join("./uploads", filename)

	// Open or create the destination file with write-only permissions
	dstFile, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error opening file for writing: %v", err)
		return
	}
	defer dstFile.Close()

	// Copy the uploaded file contents to the destination file
	if _, err := io.Copy(dstFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error copying file contents: %v", err)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "PDF file '%s' uploaded successfully", filename)
}

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}
