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
    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Expect file name in ?file=...
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "file parameter required", http.StatusBadRequest)
        return
    }

    // Disallow absolute paths coming from the client
    if filepath.IsAbs(fname) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Base directory for images (relative to current working directory)
    baseDir := "images"

    // Clean the requested path to remove ../ sequences
    // Prepend a slash so Clean resolves leading ../ correctly, then trim it
    cleanRel := strings.TrimPrefix(filepath.Clean("/"+fname), "/")

    // Join with baseDir
    requestedPath := filepath.Join(baseDir, cleanRel)

    // Resolve to absolute paths to reliably check containment
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    absReq, err := filepath.Abs(requestedPath)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is inside the base directory (prevent path traversal)
    if absReq != absBase && !strings.HasPrefix(absReq, absBase+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // Check file exists and is not a directory
    info, err := os.Stat(absReq)
    if err != nil {
        http.NotFound(w, r)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusBadRequest)
        return
    }

    // Serve the file (http.ServeFile will set appropriate headers)
    http.ServeFile(w, r, absReq)
}
