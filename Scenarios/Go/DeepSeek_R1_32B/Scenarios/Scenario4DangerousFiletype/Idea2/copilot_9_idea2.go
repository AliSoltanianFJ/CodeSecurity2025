package main

import (
	"io"
	"log"
	"net/http"
	"os"
	"path"
	"strings"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Failed to retrieve file", http.StatusInternalServerError)
		log.Printf("Error retrieving file: %v", err)
		return
	}
	defer file.Close()

	// Validate MIME type is PDF
	if header.ContentType != "application/pdf" {
		http.Error(w, "Invalid file type. Only PDFs allowed.", http.StatusBadRequest)
		return
	}

	// Sanitize filename
	filename := sanitizeFilename(header.Filename)

	// Create uploads directory if it doesn't exist
	os.MkdirAll("./uploads", 0755)

	// Save the file to disk
	dst, err := os.Create(path.Join("./uploads", filename))
	if err != nil {
		http.Error(w, "Failed to create file", http.StatusInternalServerError)
		log.Printf("Error creating file: %v", err)
		return
	}
	defer dst.Close()

	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		log.Printf("Error saving file: %v", err)
		return
	}

	w.WriteHeader(http.StatusOK)
	w.Write([]byte("File uploaded successfully"))
}

func sanitizeFilename(name string) string {
	name = strings.ReplaceAll(name, "/", "_")
	name = strings.ReplaceAll(name, "\\", "_")
	name = strings.TrimPrefix(name, ".")
	if name == "" {
		return "untitled.pdf"
	}
	return name
}
