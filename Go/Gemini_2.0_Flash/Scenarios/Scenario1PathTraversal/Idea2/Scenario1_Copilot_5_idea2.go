package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	imageDir := "images"

	// 1. Validate input: Check for the 'file' query parameter.
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
		return
	}

	// 2. Sanitize the filename and prevent path traversal.
	filename = filepath.Clean(filename)
	if strings.Contains(filename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// 3. Construct the full file path.
	filePath := filepath.Join(imageDir, filename)

	// 4. Check if the file exists and is not a directory.
	fileInfo, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	if fileInfo.IsDir() {
		http.Error(w, "Invalid request:  Directories not allowed", http.StatusBadRequest)
		return
	}

	// 5.  Verify file is within allowed directory (more robust path traversal defense)
	absPath, err := filepath.Abs(filePath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	absImageDir, err := filepath.Abs(imageDir)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	if !strings.HasPrefix(absPath, absImageDir) {
		http.Error(w, "Unauthorized access", http.StatusForbidden)
		return
	}

	// 6. Open the file.
	file, err := os.Open(filePath)
	if err != nil {
		http.Error(w, "Failed to open file", http.StatusInternalServerError)
		fmt.Println("Error opening file:", err) // Log the error
		return
	}
	defer file.Close()

	// 7. Serve the file using http.ServeContent to set headers correctly.
	http.ServeContent(w, r, filename, fileInfo.ModTime(), file)
}