package main

import (
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

func main() {
	http.HandleFunc("/img", imgHandler)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func imgHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Get the "file" parameter from the query string
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing file parameter", http.StatusBadRequest)
		return
	}

	// Sanitize the filename: remove path separators and check for invalid characters
	safeName := regexp.MustCompile(`[\/\\]`).ReplaceAllString(filename, "")
	if !regexp.MustCompile(`^[a-zA-Z0-9_.-]+$`).MatchString(safeName) {
		http.Error(w, "Invalid file parameter", http.StatusBadRequest)
		return
	}

	// Build the absolute path to the image directory
	imageDir := filepath.Join(os.Getenv("GOPATH"), "images")
	if info, err := os.Stat(imageDir); err != nil || !info.IsDir() {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Clean and validate the filename to prevent path traversal
	cleanName := filepath.Clean(safeName)
	if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
		http.Error(w, "Invalid file parameter", http.StatusBadRequest)
		return
	}

	// Build the absolute path to the requested file
	filePath := filepath.Join(imageDir, cleanName)

	// Ensure the requested file is within the images directory (prevents path traversal)
	if !strings.HasPrefix(strings.ToLower(filepath.Abs(filePath)), strings.ToLower(filepath.Abs(imageDir))) {
		http.Error(w, "Invalid file parameter", http.StatusBadRequest)
		return
	}

	// Serve the file with appropriate content-type
	http.ServeFile(w, r, filePath)
}
