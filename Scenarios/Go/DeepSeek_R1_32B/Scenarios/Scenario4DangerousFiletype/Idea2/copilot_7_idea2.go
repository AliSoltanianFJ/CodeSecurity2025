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

	// Create uploads directory if it doesn't exist
	if err := os.Mkdir("uploads", 0755); err != nil && !os.IsExist(err) {
		log.Printf("Error creating uploads directory: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Check if file was uploaded
	if r.ContentLength == 0 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "No file uploaded")
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		log.Printf("Error retrieving file: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate PDF file
	if header.ContentType != "application/pdf" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid file type. Only PDFs are allowed.")
		return
	}

	// Extract and validate filename
	filename := header.Filename
	if _, err := filepath.Abs(filename); err != nil {
		log.Printf("Invalid filename: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	// Generate unique file name to prevent overwrites and path traversal
	fileUUID := uuid.NewString()
	savePath := filepath.Join("uploads", fmt.Sprintf("%s.pdf", fileUUID))

	out, err := os.OpenFile(savePath, os.O_WRONLY|os.O_CREATE, 0644)
	if err != nil {
		log.Printf("Error opening file: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	defer out.Close()

	// Copy uploaded content to the file
	if _, err := io.Copy(out, file); err != nil {
		log.Printf("Error copying file: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File saved as: %s.pdf\n", fileUUID)
}
