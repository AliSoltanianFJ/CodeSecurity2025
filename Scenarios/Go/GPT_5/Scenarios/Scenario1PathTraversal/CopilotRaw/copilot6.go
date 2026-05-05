// ...existing code...
package main

import (
    "io"
    "log"
    "mime"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    // Read file name from query parameter "file"
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // Allow only simple filenames (no path separators). Adjust regex if you want to allow other chars.
    validFilename := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validFilename.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Base images directory (relative to working dir)
    baseDir := "images"

    // Resolve absolute paths and ensure no path traversal outside baseDir
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    target := filepath.Join(absBase, name)
    absTarget, err := filepath.Abs(target)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    // Ensure absTarget is inside absBase
    if absTarget != absBase && !strings.HasPrefix(absTarget, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    // Check file exists and is not a directory
    info, err := os.Stat(absTarget)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.NotFound(w, r)
        return
    }

    // Set content-type based on extension
    ext := strings.ToLower(filepath.Ext(name))
    if ctype := mime.TypeByExtension(ext); ctype != "" {
        w.Header().Set("Content-Type", ctype)
    } else {
        w.Header().Set("Content-Type", "application/octet-stream")
    }

    // Serve file
    f, err := os.Open(absTarget)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    if _, err := io.Copy(w, f); err != nil {
        // Log internal error but don't expose details to client
        log.Printf("error serving file %s: %v", absTarget, err)
    }
}
// ...existing code...
