package main

import (
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

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	// Ensure uploads directory exists
	uploadDir := "./uploads"
	if err := os.MkdirAll(uploadDir, 0755); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Only allow POST method
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse multipart form with size limit (10MB max)
	r.ParseMultipartForm(10 << 20)
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate PDF extension and MIME type
	ext := strings.ToLower(filepath.Ext(header.Filename))
	if ext != ".pdf" {
		http.Error(w, "Only PDF files are allowed", http.StatusUnsupportedMediaType)
		return
	}
	mimeType := header.Header.Get("Content-Type")
	if mimeType != "application/pdf" {
		http.Error(w, "Invalid file type", http.StatusUnsupportedMediaType)
		return
	}

	// Sanitize filename: remove path separators and spaces
	safeName := strings.ReplaceAll(header.Filename, string(os.PathSeparator), "_")
	safeName = strings.Trim(safeName, " ")
	if len(safeName) == 0 {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Create destination file with secure name
	dstPath := filepath.Join(uploadDir, safeName)
	dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Limit file size (10MB max) and copy safely
	limitedReader := io.LimitReader(file, 10<<20)
	_, err = io.Copy(dst, limitedReader)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	// Return success response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"filename": safeName})
}
