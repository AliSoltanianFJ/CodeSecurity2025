package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"

	"github.com/google/uuid"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse the multipart form
	r.ParseMultipartForm(10 << 20) // 10MB max upload

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "No file uploaded", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate the MIME type is PDF
	if header.ContentType != "application/pdf" {
		http.Error(w, "Invalid file type. Only PDFs are allowed.", http.StatusUnsupportedMediaType)
		return
	}

	// Create uploads directory if not exists
	if err := os.MkdirAll("uploads", 0755); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Generate a unique filename to prevent collisions and path traversal
	filename := uuid.New().String() + ".pdf"
	dstPath := filepath.Join("uploads", filename)

	// Open the destination file
	dst, err := os.Create(dstPath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// Copy the contents from the uploaded file to the destination
	if _, err = io.Copy(dst, file); err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	// Clean up temporary files if any
	r.MultipartForm.RemoveAll()

	// Return success response
	response := struct {
		Status  string `json:"status"`
		Message string `json:"message"`
		FileID  string `json:"file_id"`
	}{
		Status:  "success",
		Message: "File uploaded successfully",
		FileID:  filename,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}
