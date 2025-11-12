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

 // Allow only alphanumeric, dot, dash, and underscore
 if !isValidFilename(filename) {
 http.Error(w, "Invalid filename", http.StatusBadRequest)
 return
 }

 // Clean and join path to prevent path traversal
 imgPath := filepath.Join("images", filepath.Clean(filename))
 absImgPath, err := filepath.Abs(imgPath)
 if err != nil {
 http.Error(w, "Invalid path", http.StatusBadRequest)
 return
 }

 // Ensure the file is under the images directory
 absBase, err := filepath.Abs("images")
 if err != nil || !strings.HasPrefix(absImgPath, absBase) {
 http.Error(w, "Unauthorized access", http.StatusForbidden)
 return
 }

 f, err := os.Open(absImgPath)
 if err != nil {
 http.Error(w, "File not found", http.StatusNotFound)
 return
 }
 defer f.Close()

 // Set appropriate Content-Type based on file extension
 switch strings.ToLower(filepath.Ext(filename)) {
 case ".jpg", ".jpeg":
 w.Header().Set("Content-Type", "image/jpeg")
 case ".png":
 w.Header().Set("Content-Type", "image/png")
 case ".gif":
 w.Header().Set("Content-Type", "image/gif")
 default:
 w.Header().Set("Content-Type", "application/octet-stream")
 }

 io.Copy(w, f)
}

// isValidFilename allows only safe characters in filenames
func isValidFilename(name string) bool {
 validName := regexp.MustCompile(`^[a-zA-Z0-9._-]+$`)
 return validName.MatchString(name)
}
