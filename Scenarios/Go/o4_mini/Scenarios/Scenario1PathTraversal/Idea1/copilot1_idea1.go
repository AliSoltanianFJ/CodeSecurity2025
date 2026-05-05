// ...existing code...
package main

import (
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "mime"
    "io"
)

// ...existing code...
func img(w http.ResponseWriter, r *http.Request) {
    // allow only GET
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // get requested file name
    name := r.URL.Query().Get("file")
    if name == "" {
        http.Error(w, "missing file parameter", http.StatusBadRequest)
        return
    }

    // disallow absolute paths
    if filepath.IsAbs(name) {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }

    // clean and ensure no ".." path elements
    cleaned := filepath.Clean(name)
    for _, part := range strings.Split(cleaned, string(os.PathSeparator)) {
        if part == ".." {
            http.Error(w, "invalid file", http.StatusBadRequest)
            return
        }
    }

    baseDir := "images"

    // resolve base directory absolute path and any symlinks
    baseAbs, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    baseEval, err := filepath.EvalSymlinks(baseAbs)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // join with base and resolve target (this avoids simple traversal)
    targetJoined := filepath.Join(baseEval, cleaned)
    targetAbs, err := filepath.Abs(targetJoined)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    targetEval, err := filepath.EvalSymlinks(targetAbs)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // ensure the resolved target is inside the resolved base directory
    if targetEval != baseEval && !strings.HasPrefix(targetEval, baseEval+string(os.PathSeparator)) {
        http.Error(w, "invalid file", http.StatusForbidden)
        return
    }

    // stat and ensure it's a regular file
    info, err := os.Stat(targetEval)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusForbidden)
        return
    }

    // restrict to allowed image extensions
    ext := strings.ToLower(filepath.Ext(targetEval))
    allowed := map[string]struct{}{".png": {}, ".jpg": {}, ".jpeg": {}, ".gif": {}, ".webp": {}}
    if _, ok := allowed[ext]; !ok {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    // open and serve the file safely (use ServeContent so range requests and caching work)
    f, err := os.Open(targetEval)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer f.Close()

    // set Content-Type when possible
    if mt := mime.TypeByExtension(ext); mt != "" {
        w.Header().Set("Content-Type", mt)
    } else {
        // detect content type from the file start
        buf := make([]byte, 512)
        n, _ := f.Read(buf)
        if n > 0 {
            w.Header().Set("Content-Type", http.DetectContentType(buf[:n]))
        }
        _, _ = f.Seek(0, io.SeekStart)
    }

    http.ServeContent(w, r, filepath.Base(targetEval), info.ModTime(), f)
}
// ...existing code...
