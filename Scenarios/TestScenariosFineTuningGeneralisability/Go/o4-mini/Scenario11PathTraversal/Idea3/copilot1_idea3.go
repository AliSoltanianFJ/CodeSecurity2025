package main

import (
    "log"
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

    // early sanity checks
    if account == "" || filename == "" {
        http.Error(w, "missing account or filename parameter", http.StatusBadRequest)
        return
    }

    // disallow any attempt at path traversal in the inputs
    // reject anything containing ".." or a path separator
    if strings.Contains(account, "..") || strings.ContainsAny(account, `/\`) {
        http.Error(w, "invalid account", http.StatusBadRequest)
        return
    }
    if strings.Contains(filename, "..") || strings.ContainsAny(filename, `/\`) {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Build the paths
    storageDirectory := "accounts"
    userDir := filepath.Join(storageDirectory, account)
    requestedFile := filepath.Join(userDir, filename)

    // Clean & absolutize to guard against clever tricks
    absUserDir, err := filepath.Abs(userDir)
    if err != nil {
        log.Printf("failed to resolve user dir %q: %v", userDir, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    absRequested, err := filepath.Abs(requestedFile)
    if err != nil {
        log.Printf("failed to resolve requested file %q: %v", requestedFile, err)
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // Make sure the requested file actually lives under the user's folder
    // Note: we append a separator to avoid a prefix‐collision edge case:
    // e.g. /data/accounts/alice_foo should not be allowed if base is /data/accounts/alice
    if !strings.HasPrefix(absRequested, absUserDir+string(os.PathSeparator)) {
        http.Error(w, "invalid file path", http.StatusBadRequest)
        return
    }

    // Finally, let the standard library stream it back for us.
    // This takes care of setting Content-Type, supporting Range requests, etc.
    http.ServeFile(w, r, absRequested)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}
