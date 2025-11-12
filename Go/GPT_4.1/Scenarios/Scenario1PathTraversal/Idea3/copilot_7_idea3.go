package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Get the "file" parameter from the query string
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Clean the file name to prevent directory traversal
    fileName = filepath.Clean(fileName)
    if strings.Contains(fileName, "..") || strings.HasPrefix(fileName, "/") || strings.HasPrefix(fileName, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Only allow specific image extensions
    allowedExt := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }
    ext := strings.ToLower(filepath.Ext(fileName))
    if !allowedExt[ext] {
        http.Error(w, "File type not allowed", http.StatusForbidden)
        return
    }

    // Build the full file path
    baseDir := "images"
    fullPath := filepath.Join(baseDir, fileName)

    // Ensure the file is within the images directory
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(fullPath)
    if err != nil || !strings.HasPrefix(absPath, absBase) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file
    f, err := os.Open(absPath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set the appropriate Content-Type header
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)

    // Prevent caching sensitive content
    w.Header().Set("Cache-Control", "no-store")

    // Serve the file content
    if _, err := io.Copy(w, f); err != nil {
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}