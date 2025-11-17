package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/google/uuid"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	// Get the filename from query parameters
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Filename is required", http.StatusBadRequest)
		return
	}

	// Sanitize and validate the filename
	sanitized := sanitizeFilename(filename)
	if sanitized == "" {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Construct the full path to the image file
	imagePath := filepath.Join("images", sanitized)

	// Check if file exists and is a regular file
	stat, err := os.Stat(imagePath)
	if err != nil || stat.IsDir() {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	// Set headers to prevent caching and specify content type
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="%s"`, sanitized))
	w.Header().Set("X-Content-Type-Options", "nosniff")

	// Determine the MIME type based on file extension
	contentType := getMimeType(imagePath)
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	w.Header().Set("Content-Type", contentType)

	// Serve the file with a maximum of 10MB per request to prevent abuse
	http.ServeFile(w, r, imagePath)
}

// sanitizeFilename validates and sanitizes a filename to prevent path traversal attacks
func sanitizeFilename(filename string) string {
	// Generate a new UUID
	newUUID := uuid.New()

	// Replace any disallowed characters with a hyphen
	cleaned := strings.Map(func(r rune) rune {
		if r == '-' || r == '_' || r == '.' || ('a' <= r && r <= 'z') || ('A' <= r && r <= 'Z') || ('0' <= r && r <= '9') {
			return r
		}
		return '-'
	}, filename)

	// Ensure the filename doesn't start with a hyphen
	if len(cleaned) > 0 && cleaned[0] == '-' {
		cleaned = cleaned[1:]
	}

	// Ensure the filename is not empty after cleaning
	if len(cleaned) == 0 {
		cleaned = newUUID.String()
	}

	return cleaned
}

// getMimeType returns the MIME type for a given file based on its extension
func getMimeType(file string) string {
	switch strings.ToLower(filepath.Ext(file)) {
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".gif":
		return "image/gif"
	case ".bmp":
		return "image/bmp"
	case ".webp":
		return "image/webp"
	default:
		return "application/octet-stream"
	}
}

// containsInvalidChars checks for characters that are not allowed in filenames
func containsInvalidChars(s string) bool {
	invalid := []byte{'/', '\\', '\x00', '"', '*', ':', '<', '>', '?', '|'}
	for _, c := range s {
		if c == invalid[0] || c == invalid[1] || c == invalid[2] || c == invalid[3] || c == invalid[4] || c == invalid[5] || c == invalid[6] || c == invalid[7] || c == invalid[8] || c == invalid[9] {
			return true
		}
	}
	return false
}
