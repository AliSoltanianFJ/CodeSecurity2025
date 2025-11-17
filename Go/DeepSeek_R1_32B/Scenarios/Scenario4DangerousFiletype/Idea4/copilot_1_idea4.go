package main

import (
	"crypto/md5"
	"encoding/hex"
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

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Internal server error"))
		return
	}
	defer file.Close()

	if !strings.HasSuffix(header.Filename, ".pdf") {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte("Only PDF files are allowed"))
		return
	}

	// Create uploads directory if not exists
	if err := os.MkdirAll("uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Internal server error"))
		return
	}

	savePath := filepath.Join("uploads", header.Filename)

	// Sanitize filename to prevent path traversal attacks
	if !strings.HasPrefix(savePath, "uploads/") || strings.Contains(header.Filename, "/") {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte("Invalid file name"))
		return
	}

	outFile, err := os.OpenFile(savePath, os.O_WRONLY|os.O_CREATE, 0644)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Internal server error"))
		return
	}
	defer outFile.Close()

	if _, err = io.Copy(outFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Internal server error"))
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File %s uploaded successfully", header.Filename)
}
