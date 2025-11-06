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

	// 1. Input validation: Check for the 'file' query parameter.
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
		return
	}

	// 2. Sanitize the filename to prevent path traversal.
	cleanName := filepath.Clean(filename)
	if strings.Contains(cleanName, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

    // 3.  Verify file is within allowed directory using filepath.Rel
	relPath, err := filepath.Rel(imageDir, filepath.Join(imageDir, cleanName))
	if err != nil || strings.HasPrefix(relPath, "..") {
		http.Error(w, "File not found or unauthorized", http.StatusNotFound)
		return
	}


	// 4. Construct the full file path.
	filePath := filepath.Join(imageDir, cleanName)

	// 5. Check if the file exists.
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
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

	// 7. Serve the file.  Detect content type to prevent XSS.
	http.ServeFile(w, r, filePath)
}