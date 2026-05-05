package main

import (
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	http.HandleFunc("/img", imgHandler)

	// Configure TLS
	cfg := &tls.Config{
		MinVersion:               tls.VersionTLS12,
		CurvePreferences:         []tls.CurveID{tls.CurveP521, tls.CurveP384, tls.CurveP256},
		PreferServerCipherSuites: true,
		CipherSuites: []uint16{
			tls.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
			tls.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
			tls.TLS_RSA_WITH_AES_256_GCM_SHA384,
			tls.TLS_RSA_WITH_AES_256_CBC_SHA,
		},
	}
	srv := &http.Server{
		Addr:         ":8080",
		Handler:      nil,
		TLSConfig:    cfg,
		TLSNextProto: "h2",
	}

	log.Fatal(srv.ListenAndServeTLS("server.crt", "server.key"))
}

func imgHandler(w http.ResponseWriter, r *http.Request) {
	// Input Validation
	filename := r.URL.Query().Get("filename")
	if !isValidFilename(filename) {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Construct the file path
	imagePath := filepath.Join("images", filename)

	// Check if the file exists and is a file
	fileInfo, err := os.Stat(imagePath)
	if os.IsNotExist(err) || fileInfo.IsDir() {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	if err != nil {
		log.Println("File stat error:", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Open the file
	file, err := os.Open(imagePath)
	if err != nil {
		log.Println("File open error:", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	// Detect content type
	contentType := detectContentType(file)
	w.Header().Set("Content-Type", contentType)

	// Set security headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Security-Policy", "default-src 'self'")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-XSS-Protection", "1; mode=block")

	// Serve the file
	_, err = io.Copy(w, file)
	if err != nil {
		log.Println("File copy error:", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	fmt.Println("File served:", filename)
}

func isValidFilename(filename string) bool {
	if filename == "" || len(filename) > 255 {
		return false
	}
	cleanedFilename := filepath.Clean(filename)
	if strings.Contains(cleanedFilename, "..") || strings.Contains(cleanedFilename, "/") || strings.Contains(cleanedFilename, "\\") {
		return false
	}
	return true
}

func detectContentType(file *os.File) string {
	buffer := make([]byte, 512)
	_, err := file.Read(buffer)
	if err != nil && err != io.EOF {
		log.Println("Error reading file:", err)
		return "application/octet-stream"
	}
	_, err = file.Seek(0, io.SeekStart)
	if err != nil {
		log.Println("Error seeking file:", err)
		return "application/octet-stream"
	}
	return http.DetectContentType(buffer)
}