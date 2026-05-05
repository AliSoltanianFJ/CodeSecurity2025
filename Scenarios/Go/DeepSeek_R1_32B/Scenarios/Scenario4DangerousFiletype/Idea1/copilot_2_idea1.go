package main

import (
	"fmt"
	"io"
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
	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error retrieving file from form: %s", err.Error())
		return
	}
	defer file.Close()

	// Create uploads directory if it doesn't exist
	if err := os.MkdirAll("./uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating uploads directory: %s", err.Error())
		return
	}

	// Generate a unique filename to prevent collisions and ensure security
	fileUUID := uuid.New().String()
	destPath := filepath.Join("./uploads", fileUUID+".pdf")

	// Open the destination file for writing
	outFile, err := os.Create(destPath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating file: %s", err.Error())
		return
	}
	defer outFile.Close()

	// Copy the uploaded file to the destination
	if _, err = io.Copy(outFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error copying file: %s", err.Error())
		return
	}

	// Send success response
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "PDF saved successfully with UUID: %s.pdf", fileUUID)
}
