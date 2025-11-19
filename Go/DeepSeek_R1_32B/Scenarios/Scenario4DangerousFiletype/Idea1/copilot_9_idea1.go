package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Error retrieving PDF file: "+err.Error())
		return
	}
	defer file.Close()

	contentType := header.Header.Get("Content-Type")
	if contentType != "application/pdf" {
		w.WriteHeader(http.StatusUnsupportedMediaType)
		io.WriteString(w, "Only PDF files are allowed.")
		return
	}

	fileName := strconv.FormatInt(time.Now().UnixNano(), 10) + ".pdf"
	destPath := filepath.Join("./uploads", fileName)

	if err := os.MkdirAll(filepath.Dir(destPath), 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error creating uploads directory: "+err.Error())
		return
	}

	outFile, err := os.Create(destPath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error creating file: "+err.Error())
		return
	}
	defer outFile.Close()

	if _, err = io.Copy(outFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error saving PDF file: "+err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
	io.WriteString(w, "PDF file uploaded successfully as: "+fileName)
}
