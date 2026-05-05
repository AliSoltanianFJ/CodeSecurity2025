package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	file, fileHeader, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Unable to get file: "+err.Error(), http.StatusBadRequest)
		return
	}
	defer file.Close()

	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, 0755); err != nil {
			http.Error(w, "Unable to create upload directory: "+err.Error(), http.StatusInternalServerError)
			return
		}
	}

	filePath := filepath.Join(uploadDir, fileHeader.Filename)
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Unable to create file on disk: "+err.Error(), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	if _, err = io.Copy(dst, file); err != nil {
		http.Error(w, "Unable to write file to disk: "+err.Error(), http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "File '%s' successfully uploaded to '%s'\n", fileHeader.Filename, uploadDir)
}