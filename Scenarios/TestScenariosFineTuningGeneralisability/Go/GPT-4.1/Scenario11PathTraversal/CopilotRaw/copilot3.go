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
	fmt.Println("Using account document storage")

	// Basic validation
	if account == "" || filename == "" {
		http.Error(w, "account and filename required", http.StatusBadRequest)
		return
	}

	// Secure file path
	cleanAccount := filepath.Clean(account)
	cleanFilename := filepath.Clean(filename)
	accountDir := filepath.Join(storageDirectory, cleanAccount)

	// Final document path
	docPath := filepath.Join(accountDir, cleanFilename)

	// Prevent path traversal
	if !filepath.HasPrefix(docPath, accountDir) {
		http.Error(w, "invalid path", http.StatusBadRequest)
		return
	}

	// Open the file
	file, err := os.Open(docPath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "file not found", http.StatusNotFound)
		} else {
			http.Error(w, "internal error", http.StatusInternalServerError)
		}
		return
	}
	defer file.Close()

	// Serve the file content
	http.ServeFile(w, r, docPath)
}

func main() {
	http.HandleFunc("/document", documentHandler)
	http.ListenAndServe(":8080", nil)
}