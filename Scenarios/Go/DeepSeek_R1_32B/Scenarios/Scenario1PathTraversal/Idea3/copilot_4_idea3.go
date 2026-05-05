package main

import (
	"bytes"
	"io"
	"mime"
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
		".bmp":  true,
		".svg":  true,
		".ico":  true,
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
	imageDir := "images"
	absImageDir, err := filepath.Abs(imageDir)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Build the absolute path to the requested file
	absFilePath, err := filepath.Abs(filepath.Join(imageDir, cleanName))
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Ensure the requested file is within the images directory (prevents path traversal)
	if !strings.HasPrefix(absFilePath, absImageDir+string(os.PathSeparator)) && absFilePath != absImageDir {
		http.Error(w, "Forbidden", http.StatusForbidden)
		return
	}

	// Limit the file size (e.g., 2MB max for images)
	const maxFileSize = 2 << 20 // 2 MB
	f, err := os.Open(absFilePath)
	if err != nil {
		http.Error(w, "Not Found", http.StatusNotFound)
		return
	}
	defer f.Close()

	fi, err := f.Stat()
	if err != nil || fi.Size() > maxFileSize {
		http.Error(w, "File too large", http.StatusRequestEntityTooLarge)
		return
	}

	// Serve the file with proper content type
	contentType := mime.TypeByExtension(ext)
	if contentType == "" {
		// Fallback to detect based on magic numbers
		buf := make([]byte, 512)
		_, err := io.ReadFull(f, buf)
		if err != nil {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}
		contentType = detectContentType(buf)
	}
	w.Header().Set("Content-Type", contentType)

	// Reset file pointer to beginning and serve with size limit
	f.Seek(0, io.SeekStart)
	limitedReader := io.LimitReader(f, maxFileSize)
	http.MaxBytesWriter(w, 2<<20, limitedReader)
}

// detectContentType attempts to determine the content type based on magic numbers
func detectContentType(buf []byte) string {
	if len(buf) < 8 {
		return "application/octet-stream"
	}
	switch {
	case bytes.Equal(buf[:4], []byte{0xFF, 0xD8, 0xFF, 0xE0}): // JPEG (SOI)
		return "image/jpeg"
	case bytes.Equal(buf[:4], []byte{0x89, 0x50, 0x4E, 0x47}): // PNG
		return "image/png"
	case bytes.Equal(buf[:4], []byte{0x47, 0x49, 0x46, 0x38}): // GIF
		return "image/gif"
	case bytes.HasPrefix(buf[:8], []byte{0x00, 0x00, 0x01, 0x00}): // ICO (from Windows icon format)
		return "image/x-icon"
	default:
		return "application/octet-stream"
	}
}
