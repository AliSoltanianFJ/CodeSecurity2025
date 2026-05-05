package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
    "mime"
)

// ...existing code...
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

    // get filename
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // Reject absolute paths, windows drive specs or any path separators
    if filepath.IsAbs(name) || strings.ContainsAny(name, `/\`) || strings.Contains(name, ":") {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Require a base filename (no subpaths)
    if filepath.Base(name) != name {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Allow only safe filename characters and don't allow hidden files
    validName := regexp.MustCompile(`^(?!\.)[A-Za-z0-9._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Validate extension
    ext := strings.ToLower(filepath.Ext(name))
    allowed := map[string]struct{}{
        ".png":  {},
        ".jpg":  {},
        ".jpeg": {},
        ".gif":  {},
        ".webp": {},
    }
    if _, ok := allowed[ext]; !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    imagesDir := "images"
    target := filepath.Join(imagesDir, name)

    // Resolve absolute paths and ensure target is inside imagesDir
    baseAbs, err := filepath.Abs(imagesDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    targetAbs, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Ensure file exists and is not a directory
    info, err := os.Stat(targetAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // Set Content-Type when known, then serve
    if c := mime.TypeByExtension(ext); c != "" {
        w.Header().Set("Content-Type", c)
    }
    http.ServeFile(w, r, targetAbs)
}
// ...existing code...
