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
    q := r.URL.Query().Get("file")
    if q == "" {
        http.Error(w, "missing 'file' parameter", http.StatusBadRequest)
        return
    }

    base := "images"

    absBase, err := filepath.Abs(base)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    requested := filepath.Join(base, q)
    absReq, err := filepath.Abs(requested)
    if err != nil {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // prevent path traversal: ensure requested path is inside base
    if absReq != absBase && !strings.HasPrefix(absReq, absBase+string(os.PathSeparator)) {
        http.Error(w, "forbidden", http.StatusForbidden)
        return
    }

    http.ServeFile(w, r, absReq)
}
...existing code...
