package main

import (
	"io"
	"net/http"
	"os"
	"path"
	"regexp"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseMultipartForm(10 << 20); err != nil {
		http.Error(w, "Error parsing form data", http.StatusBadRequest)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "No file uploaded or invalid file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate filename
	filename := header.Filename
	if !isSafeFilename(filename) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Create uploads directory if not exists
	if err := os.MkdirAll("uploads", 0755); err != nil {
		http.Error(w, "Failed to create uploads directory", http.StatusInternalServerError)
		return
	}

	filePath := path.Join("uploads", filename)

	outFile, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
	if err != nil {
		http.Error(w, "Failed to create file", http.StatusInternalServerError)
		return
	}
	defer outFile.Close()

	if _, err = io.Copy(outFile, file); err != nil {
		http.Error(w, "Failed to save file", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	io.WriteString(w, "File uploaded successfully")
}

func isSafeFilename(name string) bool {
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9_.]+$`, name)
	return matched
}
