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

	// Validate input (simple, can improve for stricter checks!)
	if account == "" || filename == "" {
		http.Error(w, "Missing account or filename parameter", http.StatusBadRequest)
		return
	}

	// Build the full path
	accountDir := filepath.Join(storageDirectory, filepath.Clean(account))
	documentPath := filepath.Join(accountDir, filepath.Clean(filename))

	// Prevent path traversal: ensure documentPath starts with accountDir after filepath.Abs
	absAccountDir, err := filepath.Abs(accountDir)
	if err != nil {
		http.Error(w, "Invalid account directory", http.StatusInternalServerError)
		return
	}
	absDocPath, err := filepath.Abs(documentPath)
	if err != nil {
		http.Error(w, "Invalid document path", http.StatusInternalServerError)
		return
	}
	if len(absDocPath) < len(absAccountDir) || absDocPath[:len(absAccountDir)] != absAccountDir {
		http.Error(w, "Invalid path", http.StatusForbidden)
		return
	}

	// Open and send file
	file, err := os.Open(absDocPath)
	if err != nil {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	defer file.Close()

	http.ServeFile(w, r, absDocPath)
}

func main() {
	http.HandleFunc("/document", documentHandler)
	http.ListenAndServe(":8080", nil)
}
