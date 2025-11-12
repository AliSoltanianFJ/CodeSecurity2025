package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
  if r.Method != http.MethodPost {
    http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    return
  }
  if err := r.ParseMultipartForm(10 << 20); err != nil {
    http.Error(w, "Invalid request body", http.StatusBadRequest)
    return
  }
  file, header, err := r.FormFile("pdf")
  if err != nil {
    http.Error(w, "Missing pdf file", http.StatusBadRequest)
    return
  }
  defer file.Close()

  // Ensure uploads directory exists
  uploadsDir := "./uploads"
  if err := os.MkdirAll(uploadsDir, 0755); err != nil {
    http.Error(w, "Failed to create uploads directory", http.StatusInternalServerError)
    return
  }

  // Sanitize filename to prevent path traversal
  filename := filepath.Base(header.Filename)
  ext := strings.ToLower(filepath.Ext(filename))
  if ext != ".pdf" {
    http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
    return
  }
  destPath := filepath.Join(uploadsDir, filepath.Clean(filename))
  if err := validateFilePath(destPath, uploadsDir); err != nil {
    http.Error(w, "Invalid filename", http.StatusBadRequest)
    return
  }

  // Open destination file with secure flags (no exec)
  destFile, err := os.OpenFile(destPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
  if err != nil {
    http.Error(w, "Failed to save file", http.StatusInternalServerError)
    return
  }
  defer destFile.Close()

  // Copy file content securely (no buffer overflows)
  written, err := io.CopyN(destFile, file, 10 << 20) // Limit to 10 MB
  if err != nil {
    http.Error(w, "Failed to save file", http.StatusInternalServerError)
    return
  }
  if written == 0 {
    http.Error(w, "Empty PDF file", http.StatusBadRequest)
    return
  }

  // Respond with success message (sanitize for output)
  fmt.Fprintf(w, "Uploaded: %s", html.EscapeString(filename))
}

// validateFilePath ensures destPath is within uploadsDir and does not contain path traversal
func validateFilePath(destPath, uploadsDir string) error {
  absDest, err := filepath.Abs(destPath)
  if err != nil {
    return err
  }
  absUploads, err := filepath.Abs(uploadsDir)
  if err != nil {
    return err
  }
  if !strings.HasPrefix(absDest, absUploads) {
    return errors.New("invalid path")
  }
  return nil
}