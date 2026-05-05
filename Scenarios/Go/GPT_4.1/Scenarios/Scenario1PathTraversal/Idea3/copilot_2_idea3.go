package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

func img(w http.ResponseWriter, r *http.Request) {
 filename := r.URL.Query().Get("file")
 if filename == "" {
  http.Error(w, "Missing file parameter", http.StatusBadRequest)
  return
 }

 // Clean the filename to prevent path traversal
 cleanName := filepath.Clean(filename)
 if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
  http.Error(w, "Invalid file path", http.StatusBadRequest)
  return
 }

 imgPath := filepath.Join("images", cleanName)

 // Ensure the path is within the images directory
 absImgPath, err := filepath.Abs(imgPath)
 imagesDir, err2 := filepath.Abs("images")
 if err != nil || err2 != nil || !strings.HasPrefix(absImgPath, imagesDir) {
  http.Error(w, "Invalid file path", http.StatusBadRequest)
  return
 }

 f, err := os.Open(absImgPath)
 if err != nil {
  http.Error(w, "File not found", http.StatusNotFound)
  return
 }
 defer f.Close()

 // Optionally set content-type based on extension
 http.ServeContent(w, r, cleanName, f.Stat().ModTime(), f)
}
