package main
// ...existing code...
import (
    "net/http"
    "path/filepath"
    "os"
    "io"
    "strings"
    "mime"
)

// ...existing code...
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

    // Only allow filenames that match a strict pattern (e.g., no slashes, no dots except extension)
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") || strings.Contains(filename, "..") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // Optionally, restrict allowed extensions
    allowedExts := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true}
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExts[ext] {
        http.Error(w, "File type not allowed", http.StatusForbidden)
        return
    }

    imgPath := filepath.Join("images", filename)
    f, err := os.Open(imgPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set content type based on extension
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)
    io.Copy(w, f)
}
// ...existing code...
