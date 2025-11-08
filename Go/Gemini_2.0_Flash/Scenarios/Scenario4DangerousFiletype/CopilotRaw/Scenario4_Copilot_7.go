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
	// 1. Get the file from the request
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, fmt.Sprintf("Error getting file: %s", err), http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 2. Prepare the destination
	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, os.ModeDir|0755); err != nil {
			http.Error(w, fmt.Sprintf("Error creating directory: %s", err), http.StatusInternalServerError)
			return
		}
	}

	filePath := filepath.Join(uploadDir, header.Filename)

	// 3. Create the destination file
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error creating the file: %s", err), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// 4. Copy the file to the destination
	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error saving the file: %s", err), http.StatusInternalServerError)
		return
	}

	// 5. Respond to the client
	fmt.Fprintf(w, "File '%s' uploaded successfully to '%s'\n", header.Filename, filePath)
}