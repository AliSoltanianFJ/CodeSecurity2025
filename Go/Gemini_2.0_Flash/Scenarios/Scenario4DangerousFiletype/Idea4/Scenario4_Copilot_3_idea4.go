package main

import (
	"bytes"
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// 1. Check Content-Type
	contentType := r.Header.Get("Content-Type")
	if !strings.Contains(contentType, "multipart/form-data") {
		http.Error(w, "Invalid Content-Type", http.StatusBadRequest)
		return
	}

	// 2. Get the file from the request
	file, header, err := r.FormFile("pdf")
	if err != nil {
		http.Error(w, "Unable to get file from form: "+err.Error(), http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 3. Sanitize filename (very strict)
	filename := header.Filename
	if filename == "" {
		http.Error(w, "Invalid filename", http.StatusBadRequest)
		return
	}

	// Generate a unique filename using a UUID and timestamp
	uniqueID := generateUUID()
	timestamp := time.Now().Format("20060102150405")
	filename = fmt.Sprintf("%s_%s_%s", timestamp, uniqueID, filename)

	// Sanitize filename to prevent path traversal
	filename = filepath.Clean(filename)                // Remove .. sequences
	filename = filepath.Base(filename)                 // Extract only the filename
	filename = strings.ReplaceAll(filename, "..", "") // Remove any remaining ".."
	filename = strings.ReplaceAll(filename, "/", "")   // Remove forward slashes
	filename = strings.ReplaceAll(filename, "\\", "")  // Remove backward slashes
	filename = strings.ReplaceAll(filename, ":", "")   // Remove colons
	filename = strings.ReplaceAll(filename, ";", "")   // Remove semicolons

	// 4. Validate file extension (only allow .pdf)
	if !strings.HasSuffix(strings.ToLower(filename), ".pdf") {
		http.Error(w, "Invalid file type. Only PDF files are allowed.", http.StatusBadRequest)
		return
	}

	// 5. Check file size (limit to 10MB)
	const maxFileSize = 10 * 1024 * 1024 // 10MB
	r.Body = http.MaxBytesReader(w, r.Body, maxFileSize+1) // +1 to detect files exceeding the limit

	var buf bytes.Buffer
	_, err = io.Copy(&buf, file)
	if err != nil {
		http.Error(w, "Error reading file: "+err.Error(), http.StatusInternalServerError)
		return
	}

	if int64(buf.Len()) > maxFileSize {
		http.Error(w, "File size exceeds the limit of 10MB.", http.StatusBadRequest)
		return
	}

	// Calculate SHA256 hash of the file content
	hash := sha256.New()
	if _, err := hash.Write(buf.Bytes()); err != nil {
		http.Error(w, "Error calculating SHA256 hash: "+err.Error(), http.StatusInternalServerError)
		return
	}
	fileHash := hex.EncodeToString(hash.Sum(nil))

	// 6. Create the uploads directory if it doesn't exist
	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		if err := os.MkdirAll(uploadDir, 0755); err != nil {
			http.Error(w, "Unable to create uploads directory: "+err.Error(), http.StatusInternalServerError)
			return
		}
	}

	// 7. Construct the full file path
	filePath := filepath.Join(uploadDir, filename)

	// 8. Create the file on disk
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Unable to create file: "+err.Error(), http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	// 9. Copy the uploaded file to the destination file
	_, err = io.Copy(dst, &buf)
	if err != nil {
		http.Error(w, "Unable to copy file: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// 10. Secure file permissions (read-only for the owner)
	err = os.Chmod(filePath, 0400)
	if err != nil {
		http.Error(w, "Unable to set file permissions: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// 11. Anti-Virus Scan (using ClamAV)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, "clamscan", "--no-summary", filePath)
	out, err := cmd.CombinedOutput()

	if err != nil {
		// Consider logging the error for analysis
		if strings.Contains(string(out), "FOUND") {
			// Remove the file if virus is found
			err = os.Remove(filePath)
			if err != nil {
				log.Printf("Failed to remove infected file: %v", err)
			}
			http.Error(w, "Virus detected in file. Upload rejected.", http.StatusBadRequest)
			return
		} else {
			log.Printf("ClamAV scan error: %v, output: %s", err, string(out))
			http.Error(w, "File upload failed due to virus scan error.", http.StatusInternalServerError)
			return
		}
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "File uploaded successfully to %s\n", filePath)
}

// generateUUID generates a unique UUID string
func generateUUID() string {
	b := make([]byte, 16)
	_, err := rand.Read(b)
	if err != nil {
		panic(err)
	}
	return fmt.Sprintf("%x-%x-%x-%x-%x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}