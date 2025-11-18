package main

import (
	"bytes"
	"io"
	"math/rand"
	"net/http"
	"os"
	"path/filepath"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error retrieving file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Ensure uploads directory exists
	uploadDir := "./uploads"
	if err := os.MkdirAll(uploadDir, 0755); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Generate random filename to prevent conflicts and info leakage
	fileName := generateRandomFilename() + ".pdf"
	filePath := filepath.Join(uploadDir, fileName)

	// Validate file type (PDF)
	if !isPDF(file) {
		http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
		return
	}

	// Limit file size (10MB max)
	const maxSize = 10 << 20 // 10 MB
	limitedReader := io.LimitReader(file, maxSize+1)
	_, err = io.CopyN(os.Create(filePath), limitedReader, maxSize+1)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"filename": fileName})
}

// generateRandomFilename generates a random 8-character filename for the uploaded PDF.
func generateRandomFilename() string {
	chars := "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	rand.Seed(time.Now().UnixNano())
	name := make([]byte, 8)
	for i := range name {
		name[i] = chars[rand.Intn(len(chars))]
	}
	return string(name)
}

// isPDF checks if the provided file is a valid PDF by examining its magic number.
func isPDF(f io.Reader) bool {
	buffer := make([]byte, 4)
	_, err := io.ReadFull(f, buffer)
	if err != nil || len(buffer) < 4 {
		return false
	}
	// Check for both possible PDF headers (with and without BOM)
	pdfHeader1 := []byte{0x25, 0x50, 0x44, 0x46}
	pdfHeader2 := []byte{0xFF, 0xFE, 0x25, 0x50} // UTF-16 BOM followed by '%PDF'
	return bytes.Equal(buffer[:4], pdfHeader1) || bytes.Equal(buffer[:4], pdfHeader2)
}
