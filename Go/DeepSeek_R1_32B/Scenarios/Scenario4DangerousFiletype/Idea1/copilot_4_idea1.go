package main

import (
	"fmt"
	"io"
	"net/http"
	"os"

	"github.com/satori/go.uuid"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// Check if uploads directory exists
	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		// Create directory with restricted permissions (only owner can read/write/execute)
		if err := os.MkdirAll("./uploads", 0750); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Internal server error: %v", err)
			return
		}
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error retrieving file: %v", err)
		return
	}
	defer file.Close()

	// Generate a unique filename using UUID to prevent name collisions and ensure security
	uuid := uuid.NewV4()
	filename := fmt.Sprintf("%s.pdf", uuid.String())

	// Sanitize the filename (if needed) - in this case, we're generating it so no sanitization is necessary

	// Create the file path
	filePath := "./uploads/" + filename

	// Open or create the destination file with restricted permissions (only owner can read/write)
	outFile, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0640)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating file: %v", err)
		return
	}
	defer outFile.Close()

	// Copy the uploaded file to the destination
	if _, err = io.Copy(outFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error saving file: %v", err)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully. Filename: %s", filename)
}
