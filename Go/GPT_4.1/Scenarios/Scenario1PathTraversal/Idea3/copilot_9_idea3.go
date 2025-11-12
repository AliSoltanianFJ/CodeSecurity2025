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
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

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
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }
    ext := strings.ToLower(filepath.Ext(fileName))
    if !allowedExts[ext] {
        http.Error(w, "File type not allowed", http.StatusUnsupportedMediaType)
        return
    }

    // Build the absolute path to the image directory
    baseDir := "images"
    absBaseDir, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Build the absolute path to the requested file
    absFilePath, err := filepath.Abs(filepath.Join(baseDir, fileName))
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absFilePath, absBaseDir+string(os.PathSeparator)) && absFilePath != absBaseDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file
    file, err := os.Open(absFilePath)
    if err != nil {
        if errors.Is(err, os.ErrNotExist) {
            http.Error(w, "File not found", http.StatusNotFound)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }
    defer file.Close()

    // Set appropriate Content-Type header
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        mimeType = "application/octet-stream"
    }
    w.Header().Set("Content-Type", mimeType)

    // Optionally: prevent caching (or adjust as needed)
    w.Header().Set("Cache-Control", "no-store")

    // Serve the file content
    if _, err := io.Copy(w, file); err != nil {
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}