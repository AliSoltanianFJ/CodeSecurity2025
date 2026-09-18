package main

import (
    "fmt"
    "io"
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

    // Create the account document storage directory if it doesn't exist
    if err := os.MkdirAll(storageDirectory, 0755); err != nil {
        fmt.Printf("Failed to create storage directory: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    accountPath := filepath.Join(storageDirectory, account)
    if _, err := os.Stat(accountPath); os.IsNotExist(err) {
        fmt.Printf("Account directory not found: %s", accountPath)
        w.WriteHeader(http.StatusNotFound)
        return
    }

    filePath := filepath.Join(accountPath, filename)
    cleanPath := filepath.Clean(filePath)
    if cleanPath != filePath {
        fmt.Printf("Invalid filename: %s", filePath)
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    if _, err := os.Stat(cleanPath); os.IsNotExist(err) {
        fmt.Printf("File not found: %s", cleanPath)
        w.WriteHeader(http.StatusNotFound)
        return
    }

    file, err := os.Open(cleanPath)
    if err != nil {
        fmt.Printf("Failed to open file: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    defer file.Close()

    content, err := io.ReadAll(file)
    if err != nil {
        fmt.Printf("Failed to read file content: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Set content type based on file extension
    contentType := "text/plain"
    if ext := filepath.Ext(cleanPath); ext != "" {
        contentType = http.DetectContentType(content)
    }

    w.Header().Set("Content-Type", contentType)
    w.WriteHeader(http.StatusOK)
    _, err = io.Copy(w, io.NopCloser(io.NewFixedSizeReader(strings.NewReader(string(content)), int64(len(content)))))
    if err != nil {
        fmt.Printf("Failed to write response: %v", err)
    }
}

func main() {
    http.HandleFunc("/document", documentHandler)
    http.ListenAndServe(":8080", nil)
}