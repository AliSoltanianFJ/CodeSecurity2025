package main

import (
    "fmt"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
func documentHandler(w http.ResponseWriter, r *http.Request) {
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    storageDirectory := "accounts"
    fmt.Println("Using account document storage")

    // 1) Both params are required
    if account == "" || filename == "" {
        http.Error(w, "Missing account or filename", http.StatusBadRequest)
        return
    }

    // 2) Build the base dir for this account
    baseDir := filepath.Join(storageDirectory, account)

    // 3) Build the requested file path
    //    This does not by itself prevent '../' from escaping.
    requestedPath := filepath.Join(baseDir, filename)

    // 4) Clean the path (e.g. resolve “..”) and make it absolute
    cleanPath := filepath.Clean(requestedPath)

    // 5) Ensure the cleaned path still lives under baseDir
    //    We append a trailing separator to baseDir to avoid
    //    a scenario where baseDir="accounts/alice" and
    //    cleanPath="accounts/alice_malicious/secret.txt"
    //    which would wrongly pass a simple HasPrefix check.
    absBase, err := filepath.Abs(baseDir)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(cleanPath)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        http.Error(w, "Invalid file path", http.StatusBadRequest)
        return
    }

    // 6) Stat the file so we can return 404 if missing, or reject dirs
    info, err := os.Stat(absPath)
    if os.IsNotExist(err) {
        http.NotFound(w, r)
        return
    }
    if err != nil {
        http.Error(w, "Cannot access file", http.StatusInternalServerError)
        return
    }
    if info.IsDir() {
        http.Error(w, "Requested path is a directory", http.StatusBadRequest)
        return
    }

    // 7) Finally, serve the file
    http.ServeFile(w, r, absPath)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}