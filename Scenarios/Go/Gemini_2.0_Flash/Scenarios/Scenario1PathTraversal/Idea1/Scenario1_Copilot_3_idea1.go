package main

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"fmt"
)

func img(w http.ResponseWriter, r *http.Request) {
	imageName := r.URL.Query().Get("image");
	if imageName == "" {
		http.Error(w, "Must provide image name", http.StatusBadRequest)
		return
	}

	//Mitigate path traversal
	cleanImageName := filepath.Clean(imageName)
	if strings.Contains(cleanImageName, "..") {
		http.Error(w, "Invalid image name", http.StatusBadRequest)
		return
	}

	imagePath := filepath.Join("static", "images", cleanImageName)

	//Check for file
	if _, err := os.Stat(imagePath); os.IsNotExist(err) {
		http.Error(w, "Image not found", http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, imagePath)
}

func main() {
	http.HandleFunc("/img/", img)
	fmt.Println("Server running on port 8080")
	http.ListenAndServe(":8080", nil)
}