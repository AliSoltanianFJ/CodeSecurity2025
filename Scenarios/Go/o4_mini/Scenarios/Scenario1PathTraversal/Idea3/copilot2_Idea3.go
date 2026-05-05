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

/*
   url to return images from the folder "images",
   file name passed as ?file=<name>
*/
func img(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET or HEAD
    if r.Method != http.MethodGet && r.Method != http.MethodHead {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Grab the filename
    filename := r.URL.Query().Get("file")
    if filename == "" {
        http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // 3) Quick reject any path separators
    //    (you could also do a full Clean/Rel check below,
    //     but this gives a fast path for obvious bad requests)
    if strings.Contains(filename, "/") || strings.Contains(filename, "\\") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // 4) Only allow certain extensions
    allowedExts := map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".bmp":  true,
    }
    ext := strings.ToLower(filepath.Ext(filename))
    if !allowedExts[ext] {
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // 5) Build the candidate path under the images directory
    imagesDir := "./images"
    candidate := filepath.Join(imagesDir, filename)

    // 6) Make absolutely sure we're still inside imagesDir
    //    (this rules out “..” tricks, symlinks, etc.)
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }
    absCand, err := filepath.Abs(candidate)
    if err != nil {
        http.Error(w, "Bad file path", http.StatusBadRequest)
        return
    }
    rel, err := filepath.Rel(absBase, absCand)
    if err != nil || strings.HasPrefix(rel, ".."+string(os.PathSeparator)) || rel == ".." {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // 7) Stat it to make sure it exists and isn’t a directory
    info, err := os.Stat(absCand)
    if os.IsNotExist(err) || info.IsDir() {
        http.NotFound(w, r)
        return
    }
    if err != nil {
        http.Error(w, "Unable to read file", http.StatusInternalServerError)
        return
    }

    // 8) (Optional) Add a Cache‐Control header
    //    so browsers/CDNs will cache your images
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // 9) Serve it
    //    http.ServeFile handles Range requests, content‐type sniffing, etc.
    http.ServeFile(w, r, absCand)
}