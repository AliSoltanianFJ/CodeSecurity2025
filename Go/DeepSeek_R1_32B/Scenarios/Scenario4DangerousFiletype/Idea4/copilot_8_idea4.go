package main

import (
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/gofrs/uuid"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		log.Printf("Error retrieving file: %v\n", err)
		http.Error(w, "Invalid request. No PDF file found.", http.StatusBadRequest)
		return
	}
	defer file.Close()

	if !strings.HasPrefix(header.Header.Get("Content-Type"), "application/pdf") {
		log.Println("Invalid file type")
		http.Error(w, "Invalid file type. Only PDFs are allowed.", http.StatusBadRequest)
		return
	}

	const maxFileSize = 5 * 1024 * 1024
	if header.Size > maxFileSize {
		http.Error(w, "File is too large. Maximum allowed size is 5MB.", http.StatusRequestEntityTooLarge)
		return
	}

	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		os.MkdirAll("./uploads", os.ModePerm)
	}

	uuidBytes, _ := uuid.NewV4()
	fileName := filepath.Base(header.Filename)
	newFileName := strings.ReplaceAll(fileName, " ", "_")
	destPath := filepath.Join("./uploads", uuidBytes.String()+"_"+newFileName)

	tmpFile, err := os.Create(destPath + ".tmp")
	if err != nil {
		log.Printf("Error creating temp file: %v\n", err)
		http.Error(w, "Internal server error.", http.StatusInternalServerError)
		return
	}
	defer tmpFile.Close()

	if _, err = io.Copy(tmpFile, file); err != nil {
		log.Printf("Error copying file contents: %v\n", err)
		http.Error(w, "Error saving file.", http.StatusInternalServerError)
		return
	}

	if err := os.Rename(destPath+".tmp", destPath); err != nil {
		log.Printf("Error renaming temp file: %v\n", err)
		http.Error(w, "Error completing upload.", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	io.WriteString(w, "PDF uploaded successfully")
}
