package main

import (
	"fmt"
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

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST method is allowed", http.StatusMethodNotAllowed)
		return
	}

	const maxFileSize = 10 << 20 // 10 MB
	err := r.ParseMultipartForm(maxFileSize)
	if err != nil {
		http.Error(w, "File size exceeds the limit", http.StatusBadRequest)
		return
	}

	file, fileHeader, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Invalid file upload", http.StatusBadRequest)
		return
	}
	defer file.Close()

	fileContentType := fileHeader.Header.Get("Content-Type")
	if fileContentType != "application/pdf" {
		http.Error(w, "Uploaded file is not a PDF", http.StatusBadRequest)
		return
	}

	buffer := make([]byte, 512)
	_, err = file.Read(buffer)
	if err != nil && err != io.EOF {
		http.Error(w, "Could not read file", http.StatusInternalServerError)
		return
	}

	fileType := http.DetectContentType(buffer)
	if fileType != "application/pdf" {
		http.Error(w, "Uploaded file is not a PDF", http.StatusBadRequest)
		return
	}

	_, err = file.Seek(0, io.SeekStart)
	if err != nil {
		http.Error(w, "Could not seek file", http.StatusInternalServerError)
		return
	}

	filename := filepath.Base(fileHeader.Filename)
	filename = strings.ReplaceAll(filename, " ", "_")
	filename = filepath.Clean(filename)

	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		err = os.MkdirAll(uploadDir, 0755)
		if err != nil {
			http.Error(w, "Could not create upload directory", http.StatusInternalServerError)
			return
		}
	}

	filePath := filepath.Join(uploadDir, filename)
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Could not create file on server", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, "Could not save file", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully to %s\n", filePath)
}