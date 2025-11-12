package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
// Serve images securely from the "images" directory based on a GET parameter.
func img(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the "file" parameter from the query string
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "Missing file parameter", http.StatusBadRequest)
        return
    }

    // Clean the file name to prevent path traversal
    fileName = filepath.Clean(fileName)
    if strings.Contains(fileName, "..") || strings.HasPrefix(fileName, "/") || strings.HasPrefix(fileName, "\\") {
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
    ext := strings.ToLower(filepath.Ext(fileName))
    if !allowedExt[ext] {
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
    absFilePath, err := filepath.Abs(filepath.Join(imageDir, fileName))
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is within the images directory (prevents path traversal)
    if !strings.HasPrefix(absFilePath, absImageDir+string(os.PathSeparator)) && absFilePath != absImageDir {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Open the file for reading (do not allow uploading or modifying files)
    file, err := os.Open(absFilePath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Error opening file", http.StatusInternalServerError)
        }
        return
    }
    defer file.Close()

    // Set appropriate security headers
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-XSS-Protection", "1; mode=block")
    w.Header().Set("Cache-Control", "public, max-age=86400, immutable")

    // Serve the file with the correct Content-Type
    http.ServeContent(w, r, fileName, fileStat(file), file)
}

// fileStat returns the FileInfo for the given file or a zero time if unavailable (for http.ServeContent)
func fileStat(f *os.File) (fi os.FileInfo) {
    fi, _ = f.Stat()
    return
}