package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
    "regexp"
)

// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
func documentHandler(w http.ResponseWriter, r *http.Request) {
    account := r.URL.Query().Get("account")
    filename := r.URL.Query().Get("filename")

    if account == "" || filename == "" {
        http.Error(w, "Missing account or filename.", http.StatusBadRequest)
        return
    }

    validName := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
    validFile := regexp.MustCompile(`^[a-zA-Z0-9_-]+\.(txt|pdf)$`)

    if !validName.MatchString(account) || !validFile.MatchString(filename) {
        http.Error(w, "Invalid account or filename.", http.StatusBadRequest)
        return
    }

    fullPath := filepath.Join("accounts", account, filename)

    file, err := os.Open(fullPath)
    if err != nil {
        http.Error(w, "File not found.", http.StatusNotFound)
        return
    }
    defer file.Close()

}
func main() {
	http.HandleFunc("/document", documentHandler)
	http.ListenAndServe(":8080", nil)
}