package main

import (
	"crypto/rand"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
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
	r.ParseMultipartForm(10 << 20) // 10 MB limit
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Unable to parse form", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Generate random filename
	randomBytes := make([]byte, 16)
	_, err = rand.Read(randomBytes)
	if err != nil {
		http.Error(w, "Unable to generate random name", http.StatusInternalServerError)
		return
	}
	filename := fmt.Sprintf("%x.pdf", randomBytes)

	// Sanitize filename
	filename = filepath.Clean(filename)

	// Upload directory
	uploadDir := "./uploads"

	// Create upload directory if it doesn't exist
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		os.MkdirAll(uploadDir, 0755)
	}

	// Secure file path
	filePath := filepath.Join(uploadDir, filename)

	// Check file extension (more robust)
	ext := strings.ToLower(filepath.Ext(header.Filename))
	if ext != ".pdf" {
		http.Error(w, "Invalid file extension", http.StatusBadRequest)
		return
	}

	// Create file on disk with secure permissions
	dst, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
	if err != nil {
		http.Error(w, "Unable to open file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy file data
	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Unable to write file", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully as: %s\n", filename)
}