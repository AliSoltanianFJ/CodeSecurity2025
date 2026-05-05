package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	const maxFileSize = 10 << 20 // 10 MB
	if err := r.ParseMultipartForm(maxFileSize); err != nil {
		http.Error(w, "File too large", http.StatusBadRequest)
		return
	}

	file, handler, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error retrieving the file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Check file type
	buf := make([]byte, 512)
	if _, err := file.Read(buf); err != nil {
		http.Error(w, "Error reading file", http.StatusInternalServerError)
		return
	}

	contentType := http.DetectContentType(buf)
	if contentType != "application/pdf" {
		http.Error(w, "File type is not PDF", http.StatusBadRequest)
		return
	}

	// Reset file read pointer
	if _, err := file.Seek(0, io.SeekStart); err != nil {
		http.Error(w, "Error reading file", http.StatusInternalServerError)
		return
	}

	// Sanitize filename
	fileName := handler.Filename
	fileName = filepath.Base(fileName)
	fileName = strings.ReplaceAll(fileName, " ", "_") // Replace spaces
	fileName = filepath.Clean(fileName)                 // Clean the path

	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, 0755); err != nil {
			http.Error(w, "Could not create directory", http.StatusInternalServerError)
			return
		}
	}

	f, err := os.OpenFile(filepath.Join(uploadDir, fileName), os.O_WRONLY|os.O_CREATE, 0666)
	if err != nil {
		http.Error(w, "Could not open file", http.StatusInternalServerError)
		return
	}
	defer f.Close()

	if _, err := io.Copy(f, file); err != nil {
		http.Error(w, "Could not save file", http.StatusInternalServerError)
		return
	}

	log.Println("File uploaded successfully")
	fmt.Fprintf(w, "File uploaded successfully\n")
}