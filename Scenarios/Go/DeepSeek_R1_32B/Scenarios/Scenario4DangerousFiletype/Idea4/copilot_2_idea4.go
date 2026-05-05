package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"

	"github.com/google/uuid"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	// Ensure uploads directory exists
	if err := os.Mkdir("uploads", 0755); os.IsExist(err) {
	} else if err != nil {
		log.Printf("Error creating uploads directory: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Check if file was uploaded
	if r.FormFile("pdf") == nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "No PDF file uploaded")
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		log.Printf("Error retrieving file: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Validate MIME type
	if header.Header.Get("Content-Type") != "application/pdf" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid file type. Only PDFs are allowed.")
		return
	}

	// Generate unique filename to prevent collisions and path traversal
	filename := uuid.NewString() + ".pdf"
	dstPath := filepath.Join("uploads", filename)

	// Create temporary file for secure upload
	tmpFile, err := os.CreateTemp("", "upload-*.pdf")
	if err != nil {
		log.Printf("Error creating temp file: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	defer tmpFile.Close()

	_, err = io.Copy(tmpFile, file)
	if err != nil {
		log.Printf("Error copying file: %v", err)
		os.Remove(tmpFile.Name())
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Check if the uploaded file is empty
	if tmpFile.Size() == 0 {
		os.Remove(tmpFile.Name())
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Empty file uploaded")
		return
	}

	// Use atomic rename to avoid race conditions
	if err := os.Rename(tmpFile.Name(), dstPath); err != nil {
		log.Printf("Error moving file: %v", err)
		os.Remove(tmpFile.Name())
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File saved as: %s\n", filename)
}
