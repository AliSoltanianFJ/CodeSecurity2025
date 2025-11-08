package main

import (
	"crypto/md5"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(w, r.Body, 10*1024*1024)
	if err := r.ParseMultipartForm(10 * 1024 * 1024); err != nil {
		http.Error(w, "File too large!", http.StatusBadRequest)
		return
	}

	file, fileHeader, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	defer file.Close()

	buff := make([]byte, 512)
	if _, err = file.Read(buff); err != nil && err != io.EOF {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	filetype := http.DetectContentType(buff)
	if filetype != "application/pdf" {
		http.Error(w, "Invalid file type", http.StatusBadRequest)
		return
	}

	if _, err = file.Seek(0, io.SeekStart); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	timestamp := time.Now().Unix()
	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	fileSignature := fmt.Sprintf("%x", hash.Sum(nil))
	if _, err = file.Seek(0, io.SeekStart); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	filename := sanitizeFilename(fileHeader.Filename)
	ext := filepath.Ext(filename)
	baseName := strings.TrimSuffix(filename, ext)
	newFilename := fmt.Sprintf("%s_%d_%s%s", baseName, timestamp, fileSignature[:8], ext)
	uploadPath := filepath.Join("./uploads", newFilename)

	outFile, err := os.Create(uploadPath)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer outFile.Close()

	_, err = io.Copy(outFile, file)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	
	fmt.Fprintf(w, "File uploaded successfully!\n")
}

func sanitizeFilename(filename string) string {
	filename = filepath.Clean(filename)
	filename = strings.ReplaceAll(filename, " ", "_")
	return filename
}