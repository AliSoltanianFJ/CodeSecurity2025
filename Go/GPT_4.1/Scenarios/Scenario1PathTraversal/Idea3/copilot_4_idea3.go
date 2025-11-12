package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

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

    // Prevent path traversal by cleaning the path and checking for separators
    cleanName := filepath.Clean(fileName)
    if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Allow only specific image file extensions (whitelist)
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".bmp":  true,
        ".webp": true,
        ".svg":  true,
    }
    ext := strings.ToLower(filepath.Ext(cleanName))
    if !allowedExts[ext] {
        http.Error(w, "File type not allowed", http.StatusUnsupportedMediaType)
        return
    }

    // Build the absolute path to the image directory
    imageDir := "images"
    absImageDir, err := filepath.Abs(imageDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Build the absolute path to the requested file
    absFilePath, err := filepath.Abs(filepath.Join(imageDir, cleanName))
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absFilePath, absImageDir+string(os.PathSeparator)) && absFilePath != absImageDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file safely
    file, err := os.Open(absFilePath)
    if err != nil {
        // Avoid leaking file existence or errors
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer file.Close()

    // Set appropriate Content-Type header
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        // Fallback to detecting content type
        buf := make([]byte, 512)
        n, _ := file.Read(buf)
        mimeType = http.DetectContentType(buf[:n])
        // Reset file pointer
        file.Seek(0, io.SeekStart)
    }
    w.Header().Set("Content-Type", mimeType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "public, max-age=3600")

    // Serve the file content
    if _, err := io.Copy(w, file); err != nil {
        // Do not leak internal errors to client
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}