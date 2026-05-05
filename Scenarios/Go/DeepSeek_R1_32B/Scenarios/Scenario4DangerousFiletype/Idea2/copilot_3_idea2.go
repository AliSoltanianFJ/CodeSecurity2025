package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path"
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
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error retrieving file: %v", err)
		return
	}
	defer file.Close()

	if header.ContentType != "application/pdf" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Invalid file type. Only PDF files are allowed.")
		return
	}

	// Create uploads directory if it doesn't exist
	if _, err := os.Stat("./uploads"); os.IsNotExist(err) {
		os.MkdirAll("./uploads", 0755)
	}

	// Generate a unique filename to prevent collisions and path traversal
	fileName := fmt.Sprintf("%d.pdf", time.Now().UnixNano())
	savePath := path.Join("./uploads", fileName)

	out, err := os.Create(savePath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error creating file: %v", err)
		return
	}
	defer out.Close()

	if _, err = io.Copy(out, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error copying file: %v", err)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "File uploaded successfully:", fileName)
}
