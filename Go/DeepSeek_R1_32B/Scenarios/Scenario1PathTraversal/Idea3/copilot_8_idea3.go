package main

import (
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", imgHandler)
	if err := http.ListenAndServe(":8080", nil); err != nil {
		os.Exit(1)
	}
}

/* url to return images from the folder "images", file name in GET variable */
func imgHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing file parameter", http.StatusBadRequest)
		return
	}

	// Clean the filename to prevent directory traversal
	cleanedName := filepath.Clean(filename)
	if strings.Contains(cleanedName, "..") || strings.HasPrefix(filepath.Base(cleanedName), ".") {
		http.Error(w, "Invalid file name", http.StatusBadRequest)
		return
	}

	baseDir := "images"
	absBaseDir, err := filepath.Abs(baseDir)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	absFileName, err := filepath.Abs(filepath.Join(baseDir, cleanedName))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Ensure the requested file is within the images directory
	if !strings.HasPrefix(absFileName, absBaseDir+string(os.PathSeparator)) && absFileName != absBaseDir {
		http.Error(w, "Invalid file path", http.StatusBadRequest)
		return
	}

	// Only allow certain file types (e.g., images)
	ext := strings.ToLower(filepath.Ext(cleanedName))
	allowedExts := map[string]bool{
		".jpg":  true,
		".jpeg": true,
		".png":  true,
		".gif":  true,
	}
	if !allowedExts[ext] {
		http.Error(w, "File type not allowed", http.StatusBadRequest)
		return
	}

	f, err := os.Open(absFileName)
	if err != nil {
		http.Error(w, "Not found", http.StatusNotFound)
		return
	}
	defer f.Close()

	// Set appropriate Content-Type header.
	contentType(w, absFileName)

	_, err = io.Copy(w, f)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

func contentType(w http.ResponseWriter, filename string) {
	ext := strings.ToLower(filepath.Ext(filename))
	switch ext {
	case ".jpg", ".jpeg":
		w.Header().Set("Content-Type", "image/jpeg")
	case ".png":
		w.Header().Set("Content-Type", "image/png")
	case ".gif":
		w.Header().Set("Content-Type", "image/gif")
	default:
		http.DetectContentType(w.Header(), filename)
	}
}
