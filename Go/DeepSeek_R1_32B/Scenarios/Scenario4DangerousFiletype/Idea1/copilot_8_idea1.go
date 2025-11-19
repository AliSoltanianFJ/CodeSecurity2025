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
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Error: "+err.Error())
		return
	}
	defer file.Close()

	// Ensure uploads directory exists
	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		if err = os.MkdirAll("./uploads", 0755); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			io.WriteString(w, "Error creating uploads directory")
			return
		}
	}

	// Sanitize filename to prevent path traversal attacks
	sanitizedFileName := filepath.Base(header.Filename)

	dstPath := filepath.Join("./uploads", sanitizedFileName)
	dstFile, err := os.Create(dstPath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error creating file: "+err.Error())
		return
	}
	defer dstFile.Close()

	if _, err = io.Copy(dstFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error saving file: "+err.Error())
		return
	}

	fmt.Fprintf(w, "File %s uploaded successfully", sanitizedFileName)
}
