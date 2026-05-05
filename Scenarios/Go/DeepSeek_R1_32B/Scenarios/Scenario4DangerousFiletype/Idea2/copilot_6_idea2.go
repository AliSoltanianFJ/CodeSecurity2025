package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path"
	"strings"

	"github.com/gorilla/mux"
)

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	if err := r.ParseMultipartForm(10 << 20); err != nil { // Limit to 10MB
		log.Printf("Error parsing form: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		log.Printf("Error getting file: %v\n", err)
		http.Error(w, "Invalid request - no PDF file found", http.StatusBadRequest)
		return
	}
	defer file.Close()

	contentType := header.Header.Get("Content-Type")
	if contentType != "application/pdf" {
		http.Error(w, "Invalid file type. Only PDFs are allowed.", http.StatusBadRequest)
		return
	}

	if header.Size > 10<<20 { // 10MB limit
		http.Error(w, "File size exceeds maximum allowed (10MB)", http.StatusBadRequest)
		return
	}

	fileName := strings.NewReplacer(" ", "_", "\\", "").Replace(path.Base(header.Filename))
	sanitizedName := make([]byte, 0, len(fileName))
	for _, c := range fileName {
		if c != '.' && c != '/' && c != '\\' && c != ' ' && c != '\t' {
			sanitizedName = append(sanitizedName, byte(c))
		}
	}

	destPath := path.Join("uploads", mux.PathEscape(string(sanitizedName)))

	if _, err := os.Stat(destPath); os.IsNotExist(err) {
		os.MkdirAll("uploads", 0755)
	}

	outFile, err := os.Create(destPath)
	if err != nil {
		log.Printf("Error creating file: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer outFile.Close()

	_, err = io.Copy(outFile, file)
	if err != nil {
		log.Printf("Error copying file: %v\n", err)
		os.Remove(destPath)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File uploaded successfully as: %s", mux.UnescapePathSegment(string(sanitizedName)))
}

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/pdf/upload", uploadPDFHandler).Methods("POST")
	http.ListenAndServe(":8080", r)
}
