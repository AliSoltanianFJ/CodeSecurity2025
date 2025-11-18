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
	w.Header().Set("Content-Type", "application/json")
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid form data", http.StatusBadRequest)
		return
	}
	defer file.Close()

	filename := filepath.Base(header.Filename)
	if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
		http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
		return
	}

	magicNumber := make([]byte, 4)
	if _, err := io.ReadFull(file, magicNumber); err != nil {
		http.Error(w, "Failed to read file", http.StatusBadRequest)
		return
	}
	if string(magicNumber) != "%PDF" {
		http.Error(w, "Invalid PDF file", http.StatusBadRequest)
		return
	}

	uploadDir := "./uploads"
	os.MkdirAll(uploadDir, 0755)

	dstPath := filepath.Join(uploadDir, filename)
	dst, err := os.OpenFile(dstPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
	if err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	written, err := io.Copy(dst, io.LimitReader(file, 10<<20))
	if err != nil {
		http.Error(w, "Failed to write file", http.StatusInternalServerError)
		return
	}
	if written == 10<<20 {
		http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"message": "PDF uploaded successfully"})
}
