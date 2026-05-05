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
	addr := ":8080"
	fmt.Printf("Server listening on %s\n", addr)
	http.ListenAndServe(addr, nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	imageDir := "images"

	// 1. Get filename, validate
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
		return
	}

	// 2. Sanitize, prevent path traversal, and prevent access to hidden files
	cleanName := filepath.Clean(filename)
	if strings.Contains(cleanName, "..") || strings.HasPrefix(filepath.Base(cleanName), ".") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// 3. Construct the full file path.
	filePath := filepath.Join(imageDir, cleanName)

	// 4. Check if the file exists
	fileInfo, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, "Error checking file", http.StatusInternalServerError)
		return
	}

	//5.  Prevent access to directories
	if fileInfo.IsDir() {
		http.Error(w, "Invalid request:  Directories not allowed", http.StatusBadRequest)
		return
	}

	// 6. Open file
	file, err := os.Open(filePath)
	if err != nil {
		http.Error(w, "Could not open file", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// 7. Determine content type (safer than relying on extension)
	contentType, err := getContentType(filePath)
	if err != nil {
		http.Error(w, "Could not determine content type", http.StatusInternalServerError)
		return
	}

	// 8. Serve content
	w.Header().Set("Content-Type", contentType)
	http.ServeFile(w, r, filePath)
}

// Helper function to determine content type
func getContentType(filePath string) (string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return "", err
	}
	defer file.Close()

	buffer := make([]byte, 512)
	_, err = file.Read(buffer)
	if err != nil {
		return "", err
	}

	contentType := http.DetectContentType(buffer)
	return contentType, nil
}