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
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	file, header, err := r.FormFile("pdf")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Error retrieving file from form\n")
		return
	}
	defer file.Close()

	contentType := header.ContentType
	if contentType != "application/pdf" {
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Invalid file type. Only PDFs are allowed.\n")
		return
	}

	timeSuffix := strconv.FormatInt(time.Now().UnixNano(), 10)
	randomString := os.Getenv("RANDOM_STRING") // Replace with your own method of generating randomness
	if randomString == "" {
		randomString = "random"
	}
	baseName := timeSuffix + "-" + randomString
	savePath := filepath.Join("./uploads", baseName+".pdf")

	if err := os.MkdirAll("./uploads", 0755); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Internal server error while creating directory\n")
		return
	}

	outFile, err := os.Create(savePath)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error creating file\n")
		return
	}
	defer outFile.Close()

	if _, err = io.Copy(outFile, file); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Error saving file\n")
		return
	}

	w.WriteHeader(http.StatusOK)
	io.WriteString(w, "PDF uploaded successfully: "+baseName+".pdf\n")
}
