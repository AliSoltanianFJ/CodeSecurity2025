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

    if account == "" || filename == "" {
        http.Error(w, "Missing account or filename parameter", http.StatusBadRequest)
        return
    }

    storageDirectory := "accounts"
    fmt.Println("Using account document storage:", storageDirectory)

    // 1) Get absolute path of the storage root
    storageRoot, err := filepath.Abs(storageDirectory)
    if err != nil {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // 2) Build and clean the account directory path
    accountDir := filepath.Join(storageRoot, account)
    accountDir, err = filepath.Abs(accountDir)
    if err != nil {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Make sure accountDir is still under storageRoot
    if !strings.HasPrefix(accountDir+string(os.PathSeparator), storageRoot+string(os.PathSeparator)) {
        http.Error(w, "Invalid account", http.StatusForbidden)
        return
    }

    // 3) Build and clean the requested file path
    filePath := filepath.Join(accountDir, filename)
    filePath, err = filepath.Abs(filePath)
    if err != nil {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Make sure filePath is still under accountDir
    if !strings.HasPrefix(filePath+string(os.PathSeparator), accountDir+string(os.PathSeparator)) {
        http.Error(w, "Invalid file path", http.StatusForbidden)
        return
    }

    // 4) Stat the file so we can return 404 if it doesn't exist
    if stat, err := os.Stat(filePath); err != nil || stat.IsDir() {
        http.Error(w, "File not found", http.StatusNotFound)
        return
    }

    // 5) Serve the file
    http.ServeFile(w, r, filePath)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}