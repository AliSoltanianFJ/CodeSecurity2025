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
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	const maxUploadSize = 10 << 20 // 10 MB
	r.ParseMultipartForm(maxUploadSize)
	file, fileHeader, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Error Retrieving the File", http.StatusBadRequest)
		return
	}
	defer file.Close()

	buffer := make([]byte, 512)
	_, err = file.Read(buffer)
	if err != nil {
		http.Error(w, "Error Reading File", http.StatusInternalServerError)
		return
	}

	fileType := http.DetectContentType(buffer)
	if fileType != "application/pdf" {
		http.Error(w, "Invalid File Type. Expected PDF", http.StatusBadRequest)
		return
	}

	_, err = file.Seek(0, io.SeekStart)
	if err != nil {
		http.Error(w, "Error Seeking File", http.StatusInternalServerError)
		return
	}

	uploadPath := "./uploads"
	err = os.MkdirAll(uploadPath, os.ModeDir|0755)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	fileName := fileHeader.Filename
	fileName = filepath.Clean(fileName)
	fileName = strings.ReplaceAll(fileName, " ", "_")

	dst, err := os.Create(filepath.Join(uploadPath, fileName))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	_, err = io.Copy(dst, file)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully Uploaded File to "+uploadPath+"/"+fileName)
}