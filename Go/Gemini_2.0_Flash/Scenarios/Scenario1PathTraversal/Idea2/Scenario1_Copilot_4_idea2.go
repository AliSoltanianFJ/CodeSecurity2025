package main

import (
	"fmt"
	"net/http"
	"io"
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

	// 1. Get filename, validate
	filename := r.URL.Query().Get("file")
	if filename == "" {
		http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
		return
	}

	// 2. Sanitize.  Prevent path traversal
	filename = filepath.Clean(filename)
	if strings.Contains(filename, "..") {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// 3. Construct path
	filePath := filepath.Join(imageDir, filename)

	// 4. Check file existence
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
	buffer := make([]byte, 512)
    _, err = file.Read(buffer)
    if err != nil && err != io.EOF{
        http.Error(w, "Error reading file", http.StatusInternalServerError)
        return
    }

    contentType := http.DetectContentType(buffer)

	// 8. Reset file read pointer
	_, err = file.Seek(0, io.SeekStart)
	if err != nil {
		http.Error(w, "Could not read file", http.StatusInternalServerError)
		return
	}
	// 9. Serve content
	w.Header().Set("Content-Type", contentType)
	io.Copy(w, file)
}