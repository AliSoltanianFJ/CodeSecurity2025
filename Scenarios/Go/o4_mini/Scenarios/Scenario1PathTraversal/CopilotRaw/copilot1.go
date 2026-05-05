// ...existing code...
package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // expect ?file=<filename>
    fileName := r.URL.Query().Get("file")
    if fileName == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // disallow any path separators to prevent directory traversal
    if strings.Contains(fileName, "/") || strings.Contains(fileName, "\\") {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // normalize to base name and validate extension
    fileName = filepath.Base(fileName)
    ext := strings.ToLower(filepath.Ext(fileName))
    allowed := map[string]bool{
        ".png":  true,
        ".jpg":  true,
        ".jpeg": true,
        ".gif":  true,
        ".webp": true,
    }
    if !allowed[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // build absolute path under the images directory and ensure it stays inside that directory
    imagesDir := "images"
    target := filepath.Join(imagesDir, fileName)

    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    // ensure target is inside images directory
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // serve the file
    http.ServeFile(w, r, absTarget)
}
// ...existing code...
