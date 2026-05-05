package main

import (
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
)

const uploadsDir = "./uploads"

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	file, header, err := r.FormFile("pdf")
	if err != nil {
		log.Printf("Error retrieving file from form: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Invalid request")
		return
	}
	defer file.Close()

	// Ensure uploads directory exists
	if _, err := os.Stat(uploadsDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadsDir, 0755); err != nil {
			log.Printf("Failed to create uploads directory: %v", err)
			w.WriteHeader(http.StatusInternalServerError)
			io.WriteString(w, "Internal server error")
			return
		}
	}

	// Sanitize the filename to prevent path traversal attacks
	sanitizedName := filepath.Base(header.Filename)

	// Create destination file path with absolute path
	destPath := filepath.Join(uploadsDir, sanitizedName)

	out, err := os.Create(destPath)
	if err != nil {
		log.Printf("Failed to create file: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Internal server error")
		return
	}
	defer out.Close()

	_, err = io.Copy(out, file)
	if err != nil {
		log.Printf("Failed to copy file contents: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Internal server error")
		return
	}

	w.WriteHeader(http.StatusOK)
	io.WriteString(w, "PDF saved successfully: "+sanitizedName)
}
