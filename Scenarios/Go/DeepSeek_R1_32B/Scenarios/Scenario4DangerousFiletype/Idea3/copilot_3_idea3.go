package main

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error retrieving uploaded file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Ensure uploads directory exists
	uploadDir := "./uploads"
	if err := ensureUploadsDirExists(uploadDir); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Clean and validate filename
	filename := cleanAndValidateFilename(header.Filename)
	if filename == "" {
		http.Error(w, "Invalid file name", http.StatusBadRequest)
		return
	}

	// Validate PDF MIME type (application/pdf or application/x-pdf)
	if !isValidPDFType(header.Header.Get("Content-Type")) {
		http.Error(w, "Only PDF files are allowed", http.StatusUnsupportedMediaType)
		return
	}

	// Limit file size to 10MB (adjust as needed)
	const maxFileSize = 10 << 20 // 10 MB
	limitedReader := io.LimitReader(file, maxFileSize+1)
	if err := checkFileSize(limitedReader); err != nil {
		http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
		return
	}

	// Open file for writing securely
	outputFile, err := os.OpenFile(filepath.Join(uploadDir, filename), os.O_EXCL|os.O_WRONLY, 0644)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer outputFile.Close()

	// Copy file with context to prevent hanging
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	_, err = io.Copy(outputFile, limitedReader)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}

	w.Write([]byte("PDF uploaded successfully"))
}

// Ensure uploads directory exists and is secure
func ensureUploadsDirExists(path string) error {
	stat, err := os.Stat(path)
	if err == nil && stat.IsDir() {
		// Optionally enforce directory permissions
		if err := os.Chmod(path, 0755); err != nil {
			return fmt.Errorf("failed to set directory permissions: %v", err)
		}
		return nil
	}
	// Create directory with restricted permissions
	return os.MkdirAll(path, 0755)
}

// Clean filename and validate extension
func cleanAndValidateFilename(name string) string {
	name = filepath.Base(strings.TrimSpace(name))
	if !strings.HasSuffix(strings.ToLower(name), ".pdf") {
		return ""
	}
	// Remove any path separators just in case
	name = strings.ReplaceAll(name, string(os.PathSeparator), "_")
	return name
}

// Validate PDF MIME type
func isValidPDFType(mimeType string) bool {
	switch mimeType {
	case "application/pdf", "application/x-pdf":
		return true
	default:
		return false
	}
}

// Check if file exceeds size limit (helper)
func checkFileSize(r io.Reader) error {
	_, err := io.ReadAll(r)
	if err != nil {
		return fmt.Errorf("error reading data: %v", err)
	}
	// If we reach here, the file was within size limits
	return nil
}
