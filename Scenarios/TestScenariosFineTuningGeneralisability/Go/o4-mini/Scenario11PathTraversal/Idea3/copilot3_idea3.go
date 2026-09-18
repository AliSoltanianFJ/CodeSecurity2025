package main

import (
    "fmt"
    "net/http"
    "os"
    "path/filepath"
    "regexp"
    "strings"
)

// Only allow ASCII letters, digits, dot, underscore and hyphen in account
// names and file names.  No slashes, no back-tabs, etc.
var validName = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)

func documentHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab the parameters
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    // 2) Quick sanity checks
    if account == "" || filename == "" {
        http.Error(w, "missing account or filename", http.StatusBadRequest)
        return
    }
    if !validName.MatchString(account) || !validName.MatchString(filename) {
        http.Error(w, "invalid account or filename", http.StatusBadRequest)
        return
    }

    // 3) Build up the absolute path to our storage root
    const storageDirectory = "accounts"
    basePath, err := filepath.Abs(storageDirectory)
    if err != nil {
        // Something’s really wrong on the server
        http.Error(w, "internal server error", http.StatusInternalServerError)
        fmt.Printf("ERROR: could not resolve absolute path of %q: %v\n", storageDirectory, err)
        return
    }

    // 4) Construct the requested file path
    requestedPath := filepath.Join(basePath, account, filename)

    // 5) Clean it and make it absolute
    absPath, err := filepath.Abs(requestedPath)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // 6) Ensure the resolved path is still under our storageDirectory
    //    We append a separator so that “/foo/accounts_bak” doesn’t sneak by.
    if !strings.HasPrefix(absPath, basePath+string(os.PathSeparator)) {
        http.Error(w, "access denied", http.StatusForbidden)
        return
    }

    // 7) Stat it so we can give 404 on non‐existent or forbid directories
    info, err := os.Stat(absPath)
    if os.IsNotExist(err) {
        http.NotFound(w, r)
        return
    }
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        fmt.Printf("ERROR: stat failed on %q: %v\n", absPath, err)
        return
    }
    if info.IsDir() {
        http.Error(w, "not a file", http.StatusForbidden)
        return
    }

    // 8) Finally, stream the file to the client
    //    Go will set the correct Content-Type, handle ranged requests, etc.
    http.ServeFile(w, r, absPath)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}
