package main

import (
	"encoding/hex"
	"fmt"
	"html/template"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
)

func img(w http.ResponseWriter, r *http.Request) {
	file := r.URL.Query().Get("file")
	if file == "" {
		http.Error(w, "Missing file parameter", http.StatusBadRequest)
		return
	}

	// Validate the filename to prevent path traversal and invalid characters
	validFileName := regexp.MustCompile(`^[a-zA-Z0-9._-]+$`)
	if !validFileName.MatchString(file) {
		http.Error(w, "Invalid file name", http.StatusBadRequest)
		return
	}

	safeFile := filepath.Base(file)

	// Map of allowed extensions to MIME types
	mimeTypeMap := map[string]string{
		"png":  "image/png",
		"jpg":  "image/jpeg",
		"jpeg": "image/jpeg",
		"gif":  "image/gif",
	}

	extension := filepath.Ext(safeFile)
	contentType := mimeTypeMap[extension]
	if contentType == "" {
		contentType = "application/octet-stream"
	}

	imgPath := fmt.Sprintf("images/%s", safeFile)

	file, err := os.Open(imgPath)
	if err != nil {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	defer file.Close()

	w.Header().Set("Content-Disposition", fmt.Sprintf(`inline; filename="%s"`, hex.EncodeString([]byte(safeFile))))
	w.Header().Set("Content-Type", contentType)

	io.Copy(w, file)
}

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}
