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
    // Input validation: Only allow alphanumeric, dash, underscore, dot, and jpg/png extensions
    filename := r.URL.Query().Get("file")
    if !isValidFilename(filename) {
        http.Error(w, "Invalid filename", http.StatusBadRequest)
        return
    }

    // Prevent path traversal by cleaning the path and joining with the images directory
    imagePath := filepath.Join("images", filepath.Clean(filename))
    absImagePath, err := filepath.Abs(imagePath)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Ensure the file is within the images directory
    absImagesDir, err := filepath.Abs("images")
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    if !strings.HasPrefix(absImagePath, absImagesDir) {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // Only allow jpg and png files
    ext := strings.ToLower(filepath.Ext(filename))
    var contentType string
    switch ext {
    case ".jpg", ".jpeg":
        contentType = "image/jpeg"
    case ".png":
        contentType = "image/png"
    default:
        http.Error(w, "Unsupported file type", http.StatusUnsupportedMediaType)
        return
    }

    // Open the file securely
    f, err := os.Open(absImagePath)
    if err != nil {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set security headers
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "no-store")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-XSS-Protection", "1; mode=block")

    // Serve the file content
    if _, err := io.Copy(w, f); err != nil {
        http.Error(w, "Error serving file", http.StatusInternalServerError)
        return
    }
}

// isValidFilename ensures the filename is safe and only contains allowed characters and extensions
func isValidFilename(name string) bool {
    allowed := regexp.MustCompile(`^[a-zA-Z0-9_\-\.]+$`)
    if !allowed.MatchString(name) {
        return false
    }
    ext := strings.ToLower(filepath.Ext(name))
    return ext == ".jpg" || ext == ".jpeg" || ext == ".png"
}