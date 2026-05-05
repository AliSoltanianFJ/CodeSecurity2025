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
	// Get the uploaded file
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, fmt.Sprintf("Unable to get the file: %v", err), http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Create the uploads directory if it doesn't exist
	dir := "./uploads"
	if _, err = os.Stat(dir); os.IsNotExist(err) {
		if err = os.MkdirAll(dir, 0755); err != nil {
			http.Error(w, fmt.Sprintf("Unable to create directory: %v", err), http.StatusInternalServerError)
			return
		}
	}

	// Create the file path
	filePath := filepath.Join(dir, header.Filename)

	// Create the destination file
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Unable to create file: %v", err), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy the uploaded file to the destination file
	size, err := io.Copy(dst, file)
	if err != nil {
		http.Error(w, fmt.Sprintf("Unable to copy the file: %v", err), http.StatusInternalServerError)
		return
	}

	// Respond to the client
	w.WriteHeader(http.StatusCreated)
	fmt.Fprintf(w, "File %s uploaded successfully, size: %d bytes\n", header.Filename, size)
}