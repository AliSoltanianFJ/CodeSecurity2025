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
    // Only allow GET method
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

    // Prevent path traversal by cleaning the path and checking for '..'
    cleanName := filepath.Clean(fileName)
    if strings.Contains(cleanName, "..") || strings.HasPrefix(cleanName, "/") || strings.HasPrefix(cleanName, "\\") {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Only allow specific image extensions (whitelist)
    allowedExt := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
        ".svg":  true,
    }
    ext := strings.ToLower(filepath.Ext(cleanName))
    if !allowedExt[ext] {
        http.Error(w, "File type not allowed", http.StatusForbidden)
        return
    }

    // Build the absolute path to the image
    imageDir := "images"
    absImageDir, err := filepath.Abs(imageDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absFilePath, err := filepath.Abs(filepath.Join(imageDir, cleanName))
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the file is within the images directory (prevents directory traversal)
    if !strings.HasPrefix(absFilePath, absImageDir) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file safely
    f, err := os.Open(absFilePath)
    if err != nil {
        // Do not leak file existence or errors
        http.NotFound(w, r)
        return
    }
    defer f.Close()

    // Set appropriate Content-Type header
    mimeType := mime.TypeByExtension(ext)
    if mimeType == "" {
        // Fallback to detecting content type
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        mimeType = http.DetectContentType(buf[:n])
        _, _ = f.Seek(0, io.SeekStart) // Reset file pointer
    }
    w.Header().Set("Content-Type", mimeType)
    w.Header().Set("X-Content-Type-Options", "nosniff")

    // Prevent caching of sensitive content (adjust as needed)
    w.Header().Set("Cache-Control", "private, no-store")

    // Serve the file content
    if _, err := io.Copy(w, f); err != nil {
        // Do not leak internal errors
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
}
