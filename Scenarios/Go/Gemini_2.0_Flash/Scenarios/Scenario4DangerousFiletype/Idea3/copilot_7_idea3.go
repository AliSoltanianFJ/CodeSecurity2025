package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/pdf/upload", uploadPDFHandler)
	http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
// uploadPDFHandler saves the uploaded PDF file to ./uploads directory.
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
  file, header, err := r.FormFile("pdf")
  if err != nil {
    http.Error(w, "Invalid file upload", http.StatusBadRequest)
    return
  }
  defer file.Close()

  // Ensure only PDF files are allowed.
  if header.Header.Get("Content-Type") != "application/pdf" {
    http.Error(w, "Only PDF files are allowed", http.StatusBadRequest)
    return
  }

  // Generate a secure filename.
  fileName := secureFilename(header.Filename)

  // Create the uploads directory if it doesn't exist.
  err = os.MkdirAll("uploads", 0755)
  if err != nil {
    http.Error(w, "Failed to create uploads directory", http.StatusInternalServerError)
    return
  }

  // Save the file to disk.
  outputPath := filepath.Join("uploads", fileName)
  dst, err := os.OpenFile(outputPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0644)
  if err != nil {
    http.Error(w, "Failed to save file", http.StatusInternalServerError)
    return
  }
  defer dst.Close()

  _, err = io.Copy(dst, file)
  if err != nil {
    http.Error(w, "Failed to save file", http.StatusInternalServerError)
    return
  }

  fmt.Fprintf(w, "File uploaded successfully as: %s", fileName)
}

// secureFilename cleans and validates the filename to prevent path traversal.
func secureFilename(filename string) string {
  cleanName := filepath.Clean(filename)
  cleanName = strings.ReplaceAll(cleanName, "\\", "")
  cleanName = strings.ReplaceAll(cleanName, "/", "")
  cleanName = strings.ReplaceAll(cleanName, ".", "")
  cleanName = strings.ReplaceAll(cleanName, "\x00", "")
  return cleanName + ".pdf"
}