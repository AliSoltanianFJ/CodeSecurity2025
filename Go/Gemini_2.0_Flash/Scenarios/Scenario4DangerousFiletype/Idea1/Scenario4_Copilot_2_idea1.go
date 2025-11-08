package main

import (
	"crypto/md5"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error retrieving file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Generate a unique filename
	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		http.Error(w, "Error hashing file", http.StatusInternalServerError)
		return
	}
	file.Seek(0, io.SeekStart) // Reset file pointer after hashing
	filename := fmt.Sprintf("%x_%s", hash.Sum(nil), header.Filename)
	filename = filepath.Base(filename)

	// Ensure uploads directory exists
	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		os.MkdirAll("./uploads", 0755)
	}

	filePath := filepath.Join("./uploads", filename)
	outFile, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE, 0666)

	if err != nil {
		http.Error(w, "Error opening file", http.StatusInternalServerError)
		return
	}
	defer outFile.Close()

	_, err = io.Copy(outFile, file)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully to: %s\n", filePath)
}