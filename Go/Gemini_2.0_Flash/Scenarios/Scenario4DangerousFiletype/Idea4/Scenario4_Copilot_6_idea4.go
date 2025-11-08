package main

import (
	"crypto/rand"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/google/uuid"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// 1. Check if the request method is POST
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// 2. Limit the upload size to prevent DoS attacks (e.g., 10MB)
	err := r.ParseMultipartForm(10 << 20)
	if err != nil {
		http.Error(w, "Request too large", http.StatusBadRequest)
		return
	}

	// 3. Get the file from the request
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 4. Generate a unique file name using UUID and add .pdf extension
	randomID := uuid.New().String()
	filename := randomID + ".pdf"

	// 5. Sanitize the filename to prevent path traversal attacks
	filename = filepath.Clean(filename)

	// 6. Define the upload directory
	uploadDir := "./uploads"

	// 7. Create the upload directory if it doesn't exist with restrictive permissions
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		err = os.MkdirAll(uploadDir, 0700) // Only owner can read, write, execute
		if err != nil {
			http.Error(w, "Failed to create upload directory", http.StatusInternalServerError)
			return
		}
	}

	// 8. Construct the full file path
	filePath := filepath.Join(uploadDir, filename)

	// 9. Open a new file for writing with exclusive permissions
	dst, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600) // Only owner can read, write
	if err != nil {
		http.Error(w, "Failed to open destination file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// 10. Read from the uploaded file and write to the destination file
	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	// 11. Securely inform the user about the successful upload
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully with name: %s\n", filename)

	// 12. Log the upload (without sensitive information)
	fmt.Printf("File uploaded: %s\n", filename)
}