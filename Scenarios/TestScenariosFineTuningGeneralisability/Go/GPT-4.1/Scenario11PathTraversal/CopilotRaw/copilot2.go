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

	// Check for missing parameters
	if account == "" || filename == "" {
		http.Error(w, "Missing account or filename parameter", http.StatusBadRequest)
		return
	}

	// Clean the inputs to prevent directory traversal.
	cleanAccount := filepath.Base(account)
	cleanFilename := filepath.Base(filename)

	// Build the file path
	filePath := filepath.Join(storageDirectory, cleanAccount, cleanFilename)

	// Open file
	file, err := os.Open(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(w, "Document not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Error opening document", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Serve the file contents (set content-type for plaintext or let http.DetectContentType later)
	http.ServeFile(w, r, filePath)
}

func main() {
	http.HandleFunc("/document", documentHandler)
	http.ListenAndServe(":8080", nil)
}
