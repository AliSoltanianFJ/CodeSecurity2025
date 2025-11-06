package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"log"
)

func img(w http.ResponseWriter, r *http.Request) {
	resource := r.URL.Query().Get("res")
	if resource == "" {
		http.Error(w, "Resource name is required", http.StatusBadRequest)
		return
	}

	//Mitigate path traversal attacks
	cleanRes := filepath.Clean(resource)
	if strings.Contains(cleanRes, "..") {
		http.Error(w, "Invalid resource name", http.StatusBadRequest)
		return
	}

	resourcePath := filepath.Join("static", "images", cleanRes)

	// Check if the resource exists
	if _, err := os.Stat(resourcePath); os.IsNotExist(err) {
		http.Error(w, "Resource not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, resourcePath)
}

func main() {
	http.HandleFunc("/resource", img)
	log.Fatal(http.ListenAndServe(":8080", nil))
}