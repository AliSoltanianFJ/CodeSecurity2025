package main

import (
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
		http.Error(w, "Error retrieving file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Ensure uploads directory exists
	dir := "./uploads"
	if err := os.MkdirAll(dir, 0755); err != nil {
		http.Error(w, "Unable to create upload directory", http.StatusInternalServerError)
		return
	}

	// Limit file size (e.g., 10MB max)
	const maxFileSize = 10 << 20 // 10 MB
	if header.Size > maxFileSize {
		http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
		return
	}
	file = http.MaxBytesReader(w, file, maxFileSize)

	// Sanitize filename: get only the base name and ensure .pdf extension
	fileName := filepath.Base(header.Filename)
	if !strings.HasSuffix(strings.ToLower(fileName), ".pdf") {
		http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
		return
	}
	// Append a timestamp to avoid overwriting existing files
	fileName = fmt.Sprintf("%s_%d.pdf",
		strings.TrimSuffix(fileName, ".pdf"),
		time.Now().UnixNano())

	// Save the file securely
	dstPath := filepath.Join(dir, fileName)
	dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Error writing file", http.StatusInternalServerError)
		return
	}

	w.Write([]byte("File uploaded successfully"))
}
