package main

import (
    "fmt"
    "net/http"
    "os"
    "path/filepath"
)

// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
func documentHandler(w http.ResponseWriter, r *http.Request) {
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    storageDirectory := "accounts"
    // Create path for the account's document directory
    accountDir := filepath.Join(storageDirectory, account)
    // Create path for the specific document
    documentPath := filepath.Join(accountDir, filename)

    // Check if account directory exists
    if _, err := os.Stat(accountDir); os.IsNotExist(err) {
        http.NotFound(w, r)
        return
    }

    // Check if file exists
    if _, err := os.Stat(documentPath); os.IsNotExist(err) {
        http.NotFound(w, r)
        return
    }

    // Verify that the file is within the intended directory to prevent path traversal
    if err := os.CheckPath(documentPath); err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }

    // Open the file
    file, err := os.Open(documentPath)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    defer file.Close()

    // Determine the MIME type based on the file extension
    contentType := "application/octet-stream"
    if ext := filepath.Ext(filename); ext != "" {
        contentType = os.LookupContentType(ext)
    }

    // Set headers
    w.Header().Set("Content-Type", contentType)
    w.Header().Set("Content-Length", fmt.Sprintf("%d", file.Size()))
    // Set Date header (optional)
    w.Header().Set("Date", time.Now().String())

    // Read and serve the file
    content, err := os.ReadFile(documentPath)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    w.Write(content)
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}