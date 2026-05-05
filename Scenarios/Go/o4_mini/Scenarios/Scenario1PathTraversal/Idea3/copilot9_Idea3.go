package main

import (
    "net/http"
    "path"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/img", img)
    http.ListenAndServe(":8080", nil)
}

// img serves a single image from the local "images" folder.
// It takes a "file" query parameter, e.g.
//    GET /img?file=somepic.jpg
// and returns images/somepic.jpg if it exists.
func img(w http.ResponseWriter, r *http.Request) {
    // 1) only GET
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) pull the file name out of the URL
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "missing `file` parameter", http.StatusBadRequest)
        return
    }

    // 3) clean it up: prepend a slash so path.Clean will
    //    always give us something beginning with "/"
    //    (this also collapses e.g. "a/../b" → "/b")
    clean := path.Clean("/" + fname)

    // 4) strip off that leading slash
    //    now clean is something like "foo.jpg" or "sub/dir/bar.png"
    clean = clean[1:]

    // 5) (optional) enforce a small whitelist of extensions
    //    so even if an attacker tricks something through,
    //    we never serve .go, .exe, .html, etc.
    ext := strings.ToLower(filepath.Ext(clean))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif":
        // ok
    default:
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // 6) build the absolute paths for both the base dir and the target file
    const imageDir = "images"
    absBase, err := filepath.Abs(imageDir)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    absFile := filepath.Join(absBase, clean)
    absFile, err = filepath.Abs(absFile)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 7) make sure the requested file is still inside imageDir
    //    (this blocks any “/images/../secret.txt” style escapes)
    rel, err := filepath.Rel(absBase, absFile)
    if err != nil || strings.HasPrefix(rel, ".."+string(filepath.Separator)) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // 8) let Go’s http.ServeFile do the rest: 404, 403, mime sniffing,
    //    range support, If-Modified-Since, etc.
    http.ServeFile(w, r, absFile)
}