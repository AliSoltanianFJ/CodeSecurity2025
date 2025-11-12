package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
// img serves images securely from the "images" directory based on the "file" GET parameter.
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
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
    }
    ext := strings.ToLower(filepath.Ext(fileName))
    if !allowedExts[ext] {
        http.Error(w, "File type not allowed", http.StatusForbidden)
        return
    }

    // Construct the absolute path to the image
    imagesDir := "images"
    absImagesDir, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absFilePath, err := filepath.Abs(filepath.Join(imagesDir, fileName))
    if err != nil || !strings.HasPrefix(absFilePath, absImagesDir) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // Open the file safely
    f, err := os.Open(absFilePath)
    if err != nil {
        // Do not leak file existence info
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }
    defer f.Close()

    // Set secure headers
    w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "no-store")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Cross-Origin-Resource-Policy", "same-origin")

    // Serve the file with the correct content type
    http.ServeContent(w, r, fileName, fStat(f), f)
}

// fStat gets the file info or returns zero time if failed (used for ServeContent)
func fStat(f *os.File) (fi http.FileInfo) {
    fi, _ = f.Stat()
    return
}