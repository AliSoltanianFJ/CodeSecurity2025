package main

import (
	"io"
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
	// Set secure headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")

	// Only allow GET method
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Get the 'file' parameter from the query string
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}

	// Allow only specific image extensions (whitelist)
	allowedExts := map[string]bool{
		".jpg":  true,
		".jpeg": true,
		".png":  true,
		".gif":  true,
		".webp": true,
	}
	ext := strings.ToLower(filepath.Ext(filename))
	if !allowedExts[ext] {
		http.Error(w, "Unsupported file type", http.StatusUnsupportedMediaType)
		return
	}

	// Clean the filename to prevent path traversal
	cleanName := filepath.Clean(filename)
	if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
		http.Error(w, "Invalid file path", http.StatusBadRequest)
		return
	}

	// Build the absolute path to the image directory
	imgDir := "images"
	absImgDir, err := filepath.Abs(imgDir)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Build the absolute path to the requested file
	absFilePath, err := filepath.Abs(filepath.Join(imgDir, cleanName))
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Ensure the requested file is within the images directory (prevents path traversal)
	if !strings.HasPrefix(absFilePath, absImgDir+string(os.PathSeparator)) && absFilePath != absImgDir {
		http.Error(w, "Forbidden", http.StatusForbidden)
		return
	}

	// Open the file securely
	file, err := os.Open(absFilePath)
	if err != nil {
		// Do not leak file existence or errors
		http.Error(w, "Not Found", http.StatusNotFound)
		return
	}
	defer file.Close()

	// Set the correct Content-Type based on the file extension
	switch ext {
	case ".jpg", ".jpeg":
		w.Header().Set("Content-Type", "image/jpeg")
	case ".png":
		w.Header().Set("Content-Type", "image/png")
	case ".gif":
		w.Header().Set("Content-Type", "image/gif")
	case ".webp":
		w.Header().Set("Content-Type", "image/webp")
	default:
		w.Header().Set("Content-Type", "application/octet-stream")
	}

	// Optionally, limit the size of the file served (e.g., 10MB max)
	const maxFileSize = 10 << 20 // 10 MB
	http.ServeContent(w, r, filename, fileStat(file), io.LimitReader(file, maxFileSize))
}

// Helper to get file info safely
func fileStat(f *os.File) (fi os.FileInfo) {
	fi, _ = f.Stat()
	return
}
