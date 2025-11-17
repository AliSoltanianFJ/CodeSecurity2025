package main

import (
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		log.Printf("Error retrieving file: %v", err)
		http.Error(w, "No file uploaded", http.StatusBadRequest)
		return
	}
	defer file.Close()

	mimeType := http.DetectContentType(header.Name())
	if mimeType != "application/pdf" {
		log.Printf("Invalid file type: %s", mimeType)
		http.Error(w, "Invalid file type. Only PDFs are allowed.", http.StatusBadRequest)
		return
	}

	_UPLOADS_DIR := "./uploads"
	if _, err := os.Stat(_UPLOADS_DIR); os.IsNotExist(err) {
		if err := os.MkdirAll(_UPLOADS_DIR, 0755); err != nil {
			log.Printf("Error creating uploads directory: %v", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}

	timestamp := time.Now().UnixNano()
	hasher := md5.New()
	io.WriteString(hasher, header.Name())
	fileHash := hex.EncodeToString(hasher.Sum(nil))
	filename := fmt.Sprintf("%d_%s.pdf", timestamp, fileHash)

	savePath := filepath.Join(_UPLOADS_DIR, filename)
	newFile, err := os.Create(savePath)
	if err != nil {
		log.Printf("Error creating file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer newFile.Close()

	file.Seek(0, io.SeekStart)
	if _, err := io.Copy(newFile, file); err != nil {
		log.Printf("Error writing file: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	log.Printf("Successfully uploaded: %s", filename)
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully")
}
