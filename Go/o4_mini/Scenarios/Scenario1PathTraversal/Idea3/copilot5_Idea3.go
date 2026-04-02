package main

import (
    "net/http"
    "path"
    "path/filepath"
    "strings"
    "os"
    "log"
)

var (
    // the directory on disk where your images live
    imagesDir       = "images"
    // whitelist of extensions we will serve
    allowedExts     = map[string]bool{
        ".jpg":  true,
        ".jpeg": true,
        ".png":  true,
        ".gif":  true,
        ".webp": true,
        ".svg":  true,
    }
)

func main() {
    http.HandleFunc("/img", img)
    log.Println("Listening on :8080…")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// img handles GET /img?file=foo.png
func img(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Grab and validate the 'file' parameter
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // 3) Clean the path, and make sure it didn't try to escape
    //    we clean with a leading slash to make sure filepath.Clean
    //    always gives us an absolute‐looking path, then we strip it back off
    cleanName := path.Clean("/" + fname)[1:]
    // reject if they still tried to climb directories or embed separators
    if cleanName != fname || strings.ContainsAny(cleanName, `/\`) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // 4) enforce a strict extension whitelist
    ext := strings.ToLower(filepath.Ext(cleanName))
    if !allowedExts[ext] {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // 5) build the full path and double‐check we're still under imagesDir
    fullPath := filepath.Join(imagesDir, cleanName)
    // filepath.Clean(fullPath) should have the imagesDir as its prefix
    absBase, err := filepath.Abs(imagesDir)
    if err != nil {
        // something went horribly wrong on the server side
        log.Printf("error resolving imagesDir: %v\n", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(fullPath)
    if err != nil {
        log.Printf("error resolving image path: %v\n", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        // we ended up outside the imagesDir
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // 6) Let the stdlib do the heavy lifting (content‐type sniffing,
    //    range requests, conditional GET, etc.)
    //    http.ServeFile will also check for existence/permissions
    http.ServeFile(w, r, absPath)
}