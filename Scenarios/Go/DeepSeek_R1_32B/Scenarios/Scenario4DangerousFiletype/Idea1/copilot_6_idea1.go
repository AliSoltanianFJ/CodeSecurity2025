package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"

	"github.com/satori/go.uuid"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "No PDF file uploaded")
		return
	}
	defer file.Close()

	// Create uploads directory if it doesn't exist
	if err := os.MkdirAll("./uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Failed to create upload directory")
		return
	}

	// Generate a unique filename using UUID
	uuid := uuid.NewV4()
	fileName := uuid.String() + ".pdf"
	filePath := filepath.Join("./uploads", fileName)

	// Open the destination file for writing
	out, err := os.Create(filePath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Failed to create file")
		return
	}
	defer out.Close()

	// Copy the uploaded content to the new file
	if _, err = io.Copy(out, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Failed to save PDF file")
		return
	}

	w.WriteHeader(http.StatusOK)
	io.WriteString(w, "PDF saved successfully as "+fileName)
}
