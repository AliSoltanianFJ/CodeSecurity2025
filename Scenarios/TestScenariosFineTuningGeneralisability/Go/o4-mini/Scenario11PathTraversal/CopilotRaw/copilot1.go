
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

    // 1. Clean inputs
    safeAccount := filepath.Clean(account)
    safeFilename := filepath.Clean(filename)

    // 2. Reject anything with path‐up components or absolute paths
    if strings.HasPrefix(safeAccount, "..") || filepath.IsAbs(safeAccount) {
        http.Error(w, "Invalid account name", http.StatusBadRequest)
        return
    }
    if strings.HasPrefix(safeFilename, "..") || filepath.IsAbs(safeFilename) {
        http.Error(w, "Invalid file name", http.StatusBadRequest)
        return
    }

    // 3. Construct the full path
    fullPath := filepath.Join(storageDirectory, safeAccount, safeFilename)

    // 4. Make sure the final path is still under storageDirectory
    absBase, err := filepath.Abs(storageDirectory)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absPath, err := filepath.Abs(fullPath)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    // Ensure the requested file is within the base directory
    if !strings.HasPrefix(absPath, absBase+string(os.PathSeparator)) {
        http.Error(w, "Access denied", http.StatusForbidden)
        return
    }

    // 5. Serve the file (will return 404 if it doesn't exist)
    http.ServeFile(w, r, absPath)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}