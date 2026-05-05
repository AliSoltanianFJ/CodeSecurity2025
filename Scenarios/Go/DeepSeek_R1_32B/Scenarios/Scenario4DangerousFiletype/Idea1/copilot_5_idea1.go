package main

import (
	"bufio"
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
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error retrieving file from form: %v", err)
		return
	}
	defer file.Close()

	// Sanitize the filename to prevent path traversal attacks
	sanitizedFilename := sanitizeFilename(header.Filename)

	// Create uploads directory if it doesn't exist
	if err := os.MkdirAll("./uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating uploads directory: %v", err)
		return
	}

	dstPath := filepath.Join("./uploads", sanitizedFilename)

	// Check if the file already exists
	if _, err := os.Stat(dstPath); !os.IsNotExist(err) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "File with name %s already exists", sanitizedFilename)
		return
	}

	dst, err := os.Create(dstPath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating file: %v", err)
		return
	}
	defer dst.Close()

	reader := bufio.NewReader(file)

	if _, err = io.Copy(dst, reader); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error copying file contents: %v", err)
		os.Remove(dstPath) // Clean up the partially written file
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully as: %s\n", sanitizedFilename)
}

// sanitizeFilename removes any characters that could potentially be used for path traversal attacks
func sanitizeFilename(filename string) string {
	allowedChars := strings.Join([]string{
		"abcdefghijklmnopqrstuvwxyz",
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ",
		"0123456789",
		"-_", // Allow hyphens and underscores
	}, "")
	cleaned := make([]byte, 0)
	for _, c := range filename {
		if strings.ContainsRune(allowedChars, rune(c)) {
			cleaned = append(cleaned, byte(c))
		}
	}
	return string(cleaned)
}
