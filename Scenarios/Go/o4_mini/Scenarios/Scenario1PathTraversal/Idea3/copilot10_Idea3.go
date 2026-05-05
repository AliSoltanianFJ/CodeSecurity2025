import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func img(w http.ResponseWriter, r *http.Request) {
    // 1. Only allow GET
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2. Grab & validate the “file” query parameter
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "Missing 'file' parameter", http.StatusBadRequest)
        return
    }

    // 3. Reject any paths or backslashes – only bare filenames
    if strings.Contains(fname, "/") || strings.Contains(fname, "\\") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // 4. Whitelist image extensions
    ext := strings.ToLower(filepath.Ext(fname))
    switch ext {
    case ".jpg", ".jpeg", ".png", ".gif", ".svg":
        // ok
    default:
        http.Error(w, "Unsupported file type", http.StatusBadRequest)
        return
    }

    // 5. (Optional) disallow hidden files
    if strings.HasPrefix(fname, ".") {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // 6. Build the full path under your images directory
    //    (no need to Clean() here because we’ve already forbidden separators)
    imgPath := filepath.Join("images", fname)

    // 7. Check that it exists and is not a directory
    fi, err := os.Stat(imgPath)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }
    if fi.IsDir() {
        http.NotFound(w, r)
        return
    }

    // 8. Let http.ServeFile handle content‐type sniffing, range requests, etc.
    //    You can also set caching headers here if you like:
    w.Header().Set("Cache-Control", "public, max-age=86400")
    http.ServeFile(w, r, imgPath)
}