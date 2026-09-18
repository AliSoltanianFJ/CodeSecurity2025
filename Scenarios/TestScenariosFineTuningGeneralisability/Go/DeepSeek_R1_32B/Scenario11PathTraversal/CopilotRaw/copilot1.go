package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
)

// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
func documentHandler(w http.ResponseWriter, r *http.Request) {
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    storageDirectory := "accounts"

    // Check for missing parameters
    if account == "" || filename == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Missing account or filename parameter")
        return
    }

    // Construct the full path to the document
 filePath := filepath.Join(storageDirectory, account, filename)

    // Check if the file exists
    if _, err := os.Stat(filePath); os.IsNotExist(err) {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprintf(w, "Document not found")
        return
    }

    // Open the file for reading
    file, err := os.Open(filePath)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Error opening document")
        return
    }

    // Serve the file content
    http.ServeContent(w, r, filepath.Base(filePath), os.Stat(filePath).ModTime(), file)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}