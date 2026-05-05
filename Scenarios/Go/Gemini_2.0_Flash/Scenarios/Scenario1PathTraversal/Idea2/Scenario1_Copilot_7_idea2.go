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
	filename = filepath.Clean(filename)
	if strings.Contains(filename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

    // 3. Restrict allowed file extensions (whitelisting)
	ext := strings.ToLower(filepath.Ext(filename))
	allowedExtensions := map[string]bool{
		".jpg":  true,
		".jpeg": true,
		".png":  true,
		".gif":  true,
	}
	if !allowedExtensions[ext] {
		http.Error(w, "Invalid file type", http.StatusBadRequest)
		return
	}

	// 4. Construct the full file path.
	filePath := filepath.Join(imageDir, filename)

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
	contentType := ""
	switch ext {
	case ".jpg", ".jpeg":
		contentType = "image/jpeg"
	case ".png":
		contentType = "image/png"
	case ".gif":
		contentType = "image/gif"
	}

	w.Header().Set("Content-Type", contentType)
	http.ServeFile(w, r, filePath)
}