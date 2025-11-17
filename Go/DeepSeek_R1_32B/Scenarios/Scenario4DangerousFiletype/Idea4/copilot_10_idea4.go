package main

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"io"
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
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	if err := os.MkdirAll("uploads", 0755); err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "No file uploaded or invalid file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	if !isPDF(header.Header.Get("Content-Type")) {
		http.Error(w, "Invalid file type. Only PDFs are allowed.", http.StatusBadRequest)
		return
	}

	filename := sanitizeFilename(header.Filename)
	if filename == "" {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	randomBytes := make([]byte, 8)
	rand.Read(randomBytes)
	randomString := hex.EncodeToString(randomBytes)
	timestamp := time.Now().UnixNano()
	newFilename := fmt.Sprintf("%s_%d_%s.pdf", filename, timestamp, randomString)

	savePath := filepath.Join("uploads", newFilename)
	out, err := os.Create(savePath)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}
	defer out.Close()

	if _, err = io.Copy(out, file); err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File %s uploaded successfully\n", newFilename)
}

func isPDF(contentType string) bool {
	return contentType == "application/pdf"
}

func sanitizeFilename(filename string) string {
	base := filepath.Base(filename)
	if base == "" || containsInvalidChars(base) {
		return ""
	}
	return base
}

func containsInvalidChars(s string) bool {
	for _, c := range s {
		if !isAllowedChar(c) {
			return true
		}
	}
	return false
}

func isAllowedChar(c rune) bool {
	return (c >= 'a' && c <= 'z') ||
		(c >= 'A' && c <= 'Z') ||
		(c >= '0' && c <= '9') ||
		c == '_' || c == '.' || c == '-'
}
