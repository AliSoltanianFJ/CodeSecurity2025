package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow POST requests
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Limit request body size (e.g., 10MB max)
	r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

	// Parse multipart form with limited memory usage
	if err := r.ParseMultipartForm(1 << 20); err != nil {
		http.Error(w, "Invalid form data", http.StatusBadRequest)
		return
	}

	// Get the uploaded file
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Missing or invalid file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate file type: only allow PDF (.pdf extension) and ensure it's actually a PDF
	filename := filepath.Base(header.Filename)
	ext := strings.ToLower(filepath.Ext(filename))
	if ext != ".pdf" {
		http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
		return
	}

	// Optionally: Check the file content to verify it's a valid PDF (basic magic number check)
	buf := make([]byte, 4)
	if _, err := io.ReadFull(file, buf); err != nil {
		http.Error(w, "Invalid file", http.StatusBadRequest)
		return
	}
	if string(buf[:4]) != "%PDF" {
		http.Error(w, "Invalid PDF file", http.StatusBadRequest)
		return
	}

	// Reset file pointer to beginning after reading magic number
	if _, err := file.Seek(0, io.SeekStart); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Sanitize filename: only allow certain characters, no path traversal
	safeName := filepath.Base(sanitizeFilename(filename))
	if safeName == "" {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Create uploads directory if it doesn't exist (with proper permissions)
	uploadDir := "./uploads"
	if err := os.MkdirAll(uploadDir, 0755); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Open file for writing (with exclusive access to prevent overwriting)
	dstPath := filepath.Join(uploadDir, safeName)
	dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Limit file size while copying (10MB max)
	limReader := io.LimitReader(file, 10<<20)
	if _, err := io.Copy(dst, limReader); err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	w.Write([]byte("PDF uploaded successfully"))
}

// sanitizeFilename: allow only safe characters in filename (no path traversal)
func sanitizeFilename(name string) string {
	name = filepath.Base(name)
	// Allow only alphanumeric, dot, underscore, hyphen, and space
	re := regexp.MustCompile(`[^a-zA-Z0-9._\- ]`)
	return strings.TrimSpace(re.ReplaceAll(name, "_"))
}
