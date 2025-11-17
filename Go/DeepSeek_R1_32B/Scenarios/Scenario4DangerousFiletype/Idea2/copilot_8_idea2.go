package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Validate and sanitize the filename
	filename := path.Base(header.Filename)
	sanitizedFilename := sanitizeFileName(filename)

	// Check if the file is a PDF
	contentType := header.ContentType
	if contentType != "application/pdf" {
		http.Error(w, "Invalid File Type", http.StatusBadRequest)
		return
	}

	// Create uploads directory if it doesn't exist
	if err := os.MkdirAll("uploads", 0755); err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Save the file to the uploads directory
	filePath := path.Join("uploads", sanitizedFilename)
	outFile, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer outFile.Close()

	if _, err := io.Copy(outFile, file); err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "PDF uploaded successfully as: %s", sanitizedFilename)
}

func sanitizeFileName(name string) string {
	name = fmt.Sprintf("%v", name)
	name = path.Base(name)
	name = fmt.Sprintf("%v", name)
	name = path.Base(name)
	name = fmt.Sprintf("%v", name)

	for _, c := range []string{" ", "/", "\\", ".", ".."} {
		if path.Base(name) == c {
			name += "_file"
		}
	}

	table := make([]rune, len(name))
	for i, c := range name {
		if !isPrintable(c) || isSpace(c) || isSeparator(c) {
			table[i] = '_'
		} else {
			table[i] = c
		}
	}

	name = string(table)
	return name
}

func isPrintable(c rune) bool {
	return c >= 0x20 && c <= 0x7E
}

func isSpace(c rune) bool {
	return c == ' '
}

func isSeparator(c rune) bool {
	switch c {
	case '/', '\\', ':', '*', '?', '"', '<', '>', '|':
		return true
	default:
		return false
	}
}
