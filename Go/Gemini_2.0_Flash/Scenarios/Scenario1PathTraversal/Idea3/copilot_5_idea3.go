package main

import (
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
		http.Error(w, "Filename parameter is missing", http.StatusBadRequest)
		return
	}

	imgPath := filepath.Join("images", filename) // Construct path to image
	
	// Security: check if the image path is under the intended directory ("images") to prevent directory traversal.
	cleanPath := filepath.Clean(imgPath)
	if !isSubpath("images", cleanPath) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Open the image file
	file, err := os.Open(cleanPath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "Image not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to open image", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Set the Content-Type header based on the image type
	contentType := http.DetectContentType([]byte{}) // Use content type detection if needed
	w.Header().Set("Content-Type", contentType)

	// Serve the image data
	http.ServeContent(w, r, filename, r.Context().Value(http.ServerContextKey).(*http.Server).Now(), file) // Serve the content. 
}


// isSubpath checks if the target path is a subpath of the base path.
func isSubpath(basePath, targetPath string) bool {
	absBase, err := filepath.Abs(basePath)
	if err != nil {
		return false
	}

	absTarget, err := filepath.Abs(targetPath)
	if err != nil {
		return false
	}

	return isPrefix(absBase, absTarget)
}

func isPrefix(base, target string) bool {
	return len(target) > len(base) && target[:len(base)] == base && (len(target) == len(base) || target[len(base)] == filepath.Separator)
}