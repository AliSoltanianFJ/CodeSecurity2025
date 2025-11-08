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
	// Check method
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Limit upload size
	err := r.ParseMultipartForm(8 << 20) // 8 MB limit
	if err != nil {
		http.Error(w, "Request too large", http.StatusBadRequest)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate file extension from header
	originalFilename := header.Filename
	if !strings.HasSuffix(strings.ToLower(originalFilename), ".pdf") {
		http.Error(w, "Invalid file extension", http.StatusBadRequest)
		return
	}

	// Generate UUID
	uuid := uuid.New()
	filename := uuid.String() + ".pdf"

	// Sanitize filename
	filename = filepath.Clean(filename)

	// Upload directory
	uploadDir := "./uploads"

	// Create upload directory
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		err := os.MkdirAll(uploadDir, 0700)
		if err != nil {
			http.Error(w, "Failed to create upload directory", http.StatusInternalServerError)
			return
		}
	}

	filePath := filepath.Join(uploadDir, filename)

	// Create file with O_EXCL to prevent overwrites
	dst, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
	if err != nil {
		http.Error(w, "Failed to create file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy file
	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully: %s\n", filename)
}