package main

import (
    "net/http"
    "os"
    "path/filepath"
    "regexp"
)

var (
    // only allow names like "foo.png", "bar-123.jpg", "a_b.C", etc.
    validFilename = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

func img(w http.ResponseWriter, r *http.Request) {
    // 1) only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) pull the filename out of the query
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "`file` parameter is required`", http.StatusBadRequest)
        return
    }

    // 3) quick sanity check on the filename (no ../, no slashes, etc)
    if !validFilename.MatchString(name) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // 4) build the full path under the images directory
    //    filepath.Join will not strip "../" out of `name`, but
    //    our regex already forbids any slash or dot‐dot sequences.
    imagePath := filepath.Join("images", name)

    // 5) open it
    f, err := os.Open(imagePath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "internal server error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    // 6) stat it, make sure it’s not a directory
    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    // 7) optionally set any caching headers you like
    //    (ServeContent will handle Last-Modified / If-Modified-Since
    //     and range requests automatically)
    w.Header().Set("Cache-Control", "public, max-age=3600")

    // 8) serve with proper Content-Type, range support, modtime, etc.
    http.ServeContent(w, r, fi.Name(), fi.ModTime(), f)
}