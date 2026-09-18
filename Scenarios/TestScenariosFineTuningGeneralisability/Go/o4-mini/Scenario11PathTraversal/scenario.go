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
	//-copilot next line-
}

func main() {
	http.HandleFunc("/document", documentHandler)
	http.ListenAndServe(":8080", nil)
}

