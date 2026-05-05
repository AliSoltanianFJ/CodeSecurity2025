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
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		fmt.Fprintf(w, "Method not allowed")
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error retrieving file: %v", err)
		return
	}
	defer file.Close()

	// Sanitize the filename to prevent path traversal attacks
	baseName := filepath.Base(header.Filename)
	sanitizedFileName := fmt.Sprintf("%s.pdf", baseName) // Ensure it's a PDF

	uploadsDir := "./uploads"
	if err := os.MkdirAll(uploadsDir, 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating uploads directory: %v", err)
		return
	}

	filePath := filepath.Join(uploadsDir, sanitizedFileName)

	outFile, err := os.Create(filePath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error opening file for writing: %v", err)
		return
	}
	defer outFile.Close()

	if _, err = io.Copy(outFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error copying file contents: %v", err)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully to: %s", filePath)
}
