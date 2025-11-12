package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		http.Error(w, "Missing filename parameter", http.StatusBadRequest)
		return
	}

	// Security: Prevent directory traversal
	cleanFilename := filepath.Clean(filename)
	if filepath.IsAbs(cleanFilename) || cleanFilename != filename {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	imgPath := filepath.Join("images", cleanFilename) // Assuming "images" is the directory where images are stored

	// Check if the file exists
	if _, err := os.Stat(imgPath); os.IsNotExist(err) {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	// Open the file
	file, err := os.Open(imgPath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error opening file: %v", err), http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Set the Content-Type header based on the file extension (naive approach).
	// A more robust approach would use `http.DetectContentType` or a similar library.
	ext := filepath.Ext(cleanFilename)
	contentType := ""
	switch ext {
	case ".jpg", ".jpeg":
		contentType = "image/jpeg"
	case ".png":
		contentType = "image/png"
	case ".gif":
		contentType = "image/gif"
	default:
		contentType = "application/octet-stream" // Default if type is unknown
	}

	w.Header().Set("Content-Type", contentType)

	// Serve the file content
	http.ServeContent(w, r, cleanFilename, time.Now(), file) // Use ServeContent for efficient handling.

	/*
	   // Alternative:  Manually read and write the content
	   // This method is less efficient but included for illustration

	   // Read the file content
	   data, err := ioutil.ReadFile(imgPath)
	   if err != nil {
	      http.Error(w, fmt.Sprintf("Error reading file: %v", err), http.StatusInternalServerError)
	      return
	   }

	   // Write the content to the response
	   _, err = w.Write(data)
	   if err != nil {
	      http.Error(w, fmt.Sprintf("Error writing to response: %v", err), http.StatusInternalServerError)
	      return
	   }
	*/
}