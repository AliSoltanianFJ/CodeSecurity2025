package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
)

// Only allow safe characters in account and filename to prevent injection attacks.
var safeChars = []string{".", "-"}

// isValidName checks if a name contains only allowed characters (alphanumeric, ., -).
func isValidName(name string) bool {
    if len(name) == 0 || len(name) > 64 {
        return false
    }
    for _, r := range name {
        if !(r >= 'a' && r <= 'z') &&
            !(r >= 'A' && r <= 'Z') &&
            !(r >= '0' && r <= '9') &&
            !isIn(r, safeChars) {
            return false
        }
    }
    return true
}

// isIn checks if a character is in a slice.
func isIn(c rune, list []string) bool {
    for _, s := range list {
        if string(c) == s {
            return true
        }
    }
    return false
}

// serveFile serves a file with proper security headers and content type detection.
func serveFile(w http.ResponseWriter, path string) error {
    f, err := os.Open(path)
    if err != nil {
        return err
    }
    defer f.Close()

    // Use ServeContent to handle range requests and set correct headers.
    http.ServeContent(w, r, filepath.Base(path), f.(*os.File).Stat().ModTime(), f)
    return nil
}

// Main file serving logic with security considerations.
func documentHandler(w http.ResponseWriter, r *http.Request) {
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    // Validate input parameters for safety.
    if !isValidName(account) || !isValidName(filename) {
        http.Error(w, "Invalid parameter", http.StatusBadRequest)
        return
    }

    // Construct the absolute path to the requested file with cleaning.
    storageDir := filepath.Join("accounts", account)
    absPath, err := filepath.Abs(storageDir)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    absFilePath, err := filepath.Abs(filepath.Join(absPath, filename))
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Ensure the requested file is within the expected directory structure.
    if !isValidPath(absPath, absFilePath) {
        http.NotFound(w, r)
        return
    }

    // Open and serve the file securely.
    err = serveFile(w, absFilePath)
    if err != nil {
        http.Error(w, "Unable to serve file", http.StatusInternalServerError)
        return
    }
}

func main() {
	http.HandleFunc("/document", documentHandler)
	http.ListenAndServe(":8080", nil)
}

