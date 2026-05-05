package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

var (
    // only lower-case a–z, 0–9, dash or underscore in the name
    // followed by one of our allowed extensions
    validImage = regexp.MustCompile(`^[a-z0-9\-_]+\.(jpg|jpeg|png|gif|bmp|webp)$`)
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// img serves files out of the local "images/" directory.
// It expects a GET parameter "file" containing only the
// base filename (no path separators), and an allowed extension.
func img(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Extract and normalize the filename
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }
    // lower-case it so our regex can just check lower-case extensions
    fname = strings.ToLower(fname)

    // 3) Quick regex check to reject anything with slashes, dots, etc.
    if !validImage.MatchString(fname) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // 4) Build the full path under ./images
    //    Since fname has no slashes, filepath.Join can’t escape the directory.
    imagePath := filepath.Join("images", fname)

    // 5) Stat it to make sure it exists and is not a directory
    info, err := os.Stat(imagePath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "internal server error", http.StatusInternalServerError)
        }
        return
    }
    if info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // 6) (Optional) set caching headers
    //    here we tell browsers and CDNs they can cache for 1 day
    w.Header().Set("Cache-Control", "public, max-age=86400")

    // 7) Serve the file (will set Content-Type, handle conditional GET, ranges, etc.)
    http.ServeFile(w, r, imagePath)
}