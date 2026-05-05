package main

import (
	"bufio"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow POST method
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse multipart form with size limit (20MB max per file)
	r.Body = http.MaxBytesReader(w, r.Body, 20<<20)

	// Get the uploaded file
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error processing file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Ensure uploads directory exists with proper permissions
	uploadDir := "./uploads"
	if err := os.MkdirAll(uploadDir, 0700); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Sanitize filename: only allow .pdf extension and alphanumeric characters
	baseName := filepath.Base(header.Filename)
	nameOnly := filepath.Base(strings.TrimSuffix(baseName, ".pdf"))
	if !validFilename(nameOnly) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}
	safeName := strings.TrimPrefix(filepath.Clean(nameOnly), ".")

	// Optionally add a unique prefix to avoid overwriting existing files
	finalPath := filepath.Join(uploadDir, fmt.Sprintf("%s_%d.pdf", safeName, time.Now().UnixNano()))

	// Additional security: check for PDF magic number (%PDF-)
	scanner := bufio.NewScanner(file)
	scanner.Buffer(make([]byte, 5), 5)
	if !scanner.Scan() || strings.TrimSpace(scanner.Text()) != "%PDF-" {
		http.Error(w, "Invalid PDF file", http.StatusBadRequest)
		return
	}

	// Reset file pointer after magic number check
	if err := file.Seek(0, io.SeekStart); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Save the file with size limit (defense in depth)
	out, err := os.OpenFile(finalPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer out.Close()

	// Enforce size limit again (defense in depth)
	limitedReader := io.LimitReader(file, 20<<20)
	if _, err := io.Copy(out, limitedReader); err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}

	// Verify the file was created (optional but safe)
	if !isRegularFile(finalPath) {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	w.Write([]byte("PDF uploaded successfully"))
}

// Helper: validate filename characters (only allow .pdf extension and alphanumeric)
func validFilename(name string) bool {
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9_.-]+$`, name)
	return matched && !strings.Contains(name, "..")
}

// Helper: check if a file exists and is a regular file
func isRegularFile(path string) bool {
	info, err := os.Stat(path)
	return err == nil && info.Mode().IsRegular()
}
