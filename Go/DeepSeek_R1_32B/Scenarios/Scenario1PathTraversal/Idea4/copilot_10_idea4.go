package main

import (
	"encoding/base64"
	"fmt"
	"html"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
)

func img(w http.ResponseWriter, r *http.Request) {
	const imagesDir = "images"

	fileName := r.URL.Query().Get("file")
	if fileName == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Missing file parameter")
		return
	}

	sanitizedFileName := sanitizeFileName(fileName)
	if sanitizedFileName == "" {
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintln(w, "Invalid file name")
		return
	}

	filePath := filepath.Join(imagesDir, sanitizedFileName)

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintln(w, "File not found")
		return
	}

	if isDirectory, err := isDirectory(filePath); err != nil || isDirectory {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Invalid file")
		return
	}

	http.ServeFile(w, r, filePath)
}

func sanitizeFileName(fileName string) string {
	safeChars := regexp.MustCompile(`^[\w\-.]+$`)
	if !safeChars.MatchString(fileName) {
		return ""
	}
	return html.EscapeString(base64.StdEncoding.EncodeToString([]byte(fileName)))
}

func isDirectory(path string) (bool, error) {
	fi, err := os.Stat(path)
	if err != nil {
		return false, err
	}
	return fi.IsDir(), nil
}

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}
