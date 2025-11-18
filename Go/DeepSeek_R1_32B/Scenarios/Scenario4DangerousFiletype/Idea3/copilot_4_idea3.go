package main

import (
	"crypto/rand"
	"encoding/hex"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

const (
	maxUploadSize    = 10 * 1024 * 1024 // 10 MB
	uploadPath       = "./uploads"
	allowedMimeType  = "application/pdf"
	pdfMagicNumber   = "%PDF-"
	maxFilenameBytes = 16
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	if err := http.ListenAndServe(":8080", nil); err != nil {
		http.Error(w, "Server failed to start", http.StatusInternalServerError)
		return
	}
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+1024)
	if err := r.ParseMultipartForm(maxUploadSize); err != nil {
		http.Error(w, "File too large or invalid form data", http.StatusBadRequest)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	ext := strings.ToLower(filepath.Ext(header.Filename))
	if ext != ".pdf" {
		http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
		return
	}

	mimeType := header.Header.Get("Content-Type")
	if mimeType != allowedMimeType {
		http.Error(w, "Invalid file type", http.StatusBadRequest)
		return
	}

	// Verify PDF magic number (first 5 bytes)
	buf := make([]byte, len(pdfMagicNumber))
	if _, err := io.ReadFull(file, buf); err != nil {
		http.Error(w, "Invalid PDF file", http.StatusBadRequest)
		return
	}
	if string(buf) != pdfMagicNumber {
		http.Error(w, "Invalid PDF file signature", http.StatusBadRequest)
		return
	}

	// Reset file pointer to start after magic number verification
	if seeker, ok := file.(io.Seeker); ok {
		seeker.Seek(0, io.SeekStart)
	} else {
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}

	// Generate random filename to prevent conflicts and guessing
	randName, err := generateRandomFilename(".pdf")
	if err != nil {
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}
	destPath := filepath.Join(uploadPath, randName)

	if err := os.MkdirAll(filepath.Dir(destPath), 0700); err != nil {
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}

	dst, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
	if err != nil {
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	written, err := io.Copy(dst, io.LimitReader(file, maxUploadSize))
	if err != nil {
		os.Remove(destPath) // Clean up partial file
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	if written > maxUploadSize {
		os.Remove(destPath) // Clean up oversized file
		http.Error(w, "File too large", http.StatusBadRequest)
		return
	}

	http.WriteHeader(http.StatusNoContent)
}

// generateRandomFilename generates a secure random filename with extension
func generateRandomFilename(ext string) (string, error) {
	bytes := make([]byte, maxFilenameBytes)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}
	// Convert to hex for readability while maintaining entropy
	return hex.EncodeToString(bytes[:maxFilenameBytes/2]) + ext, nil
}
