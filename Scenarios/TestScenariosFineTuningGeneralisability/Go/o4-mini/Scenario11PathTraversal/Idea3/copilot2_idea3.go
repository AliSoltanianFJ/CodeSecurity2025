package main

import (
    "fmt"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
func documentHandler(w http.ResponseWriter, r *http.Request) {
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    // 1) basic presence check
    if account == "" || filename == "" {
        http.Error(w, "account and filename are required", http.StatusBadRequest)
        return
    }

    // 2) flatly reject any “..” components
    if strings.Contains(account, "..") || strings.Contains(filename, "..") {
        http.Error(w, "invalid characters in account or filename", http.StatusBadRequest)
        return
    }

    // 3) build the on-disk path (still potentially relative, with symlinks, etc.)
    storageDirectory := "accounts"
    requestedPath := filepath.Join(storageDirectory, account, filename)

    // 4) turn the storage root into an absolute, symlink‐free path
    absStorageRoot, err := filepath.Abs(storageDirectory)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    absStorageRoot, err = filepath.EvalSymlinks(absStorageRoot)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 5) turn the requested path into an absolute, symlink‐free path
    absRequested, err := filepath.Abs(requestedPath)
    if err != nil {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }
    absRequested, err = filepath.EvalSymlinks(absRequested)
    if err != nil {
        // either the file doesn’t exist, or it’s a broken symlink
        http.NotFound(w, r)
        return
    }

    // 6) verify that the requested file really lives under our storage directory
    //    (the trailing Separator guards against “/accountsABC” matching “/accounts”)
    if !strings.HasPrefix(absRequested, absStorageRoot+string(filepath.Separator)) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // 7) open the file, check it isn’t a directory, then serve it
    f, err := os.Open(absRequested)
    if err != nil {
        if os.IsNotExist(err) {
            http.NotFound(w, r)
        } else {
            http.Error(w, "internal server error", http.StatusInternalServerError)
        }
        return
    }
    defer f.Close()

    fi, err := f.Stat()
    if err != nil || fi.IsDir() {
        http.NotFound(w, r)
        return
    }
}