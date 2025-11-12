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
  // Get the requested file name
  fileName := r.URL.Query().Get("file")
  if fileName == "" {
    http.Error(w, "File parameter required", http.StatusBadRequest)
    return
  }

  // Ensure it's a safe file name
  fileName = filepath.Clean(fileName)
  if strings.Contains(fileName, "..") || strings.HasPrefix(fileName, "/") || strings.HasPrefix(fileName, "\\") {
    http.Error(w, "Invalid file name", http.StatusBadRequest)
    return
  }

  // Ensure the file is within the "images" folder
  imagesPath := filepath.Join("images")
  filePath := filepath.Join(imagesPath, fileName)

  // Open the file
  file, err := os.Open(filePath)
  if err != nil {
    http.Error(w, "File not found", http.StatusNotFound)
    return
  }
  defer file.Close()

  // Get file info
  fileInfo, err := file.Stat()
  if err != nil {
    http.Error(w, "Failed to get file info", http.StatusInternalServerError)
    return
  }

  // Only serve regular files, not directories
  if fileInfo.IsDir() {
    http.Error(w, "Invalid file name", http.StatusBadRequest)
    return
  }

  // Determine Content-Type
  contentType := mime.TypeByExtension(filepath.Ext(fileName))
  if contentType == "" {
    contentType = "image/jpeg" // Default
  }
  w.Header().Set("Content-Type", contentType)

  // Disable caching
  w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
  w.Header().Set("Expires", "0")
  w.Header().Set("Pragma", "no-cache")

  // Serve the file content
  http.ServeContent(w, r, fileName, fileInfo.ModTime(), file)
}