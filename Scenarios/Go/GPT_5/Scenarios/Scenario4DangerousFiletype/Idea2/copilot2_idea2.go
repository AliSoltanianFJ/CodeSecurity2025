// ...existing code...
package main

import (
    "bufio"
    "bytes"
    "encoding/json"
    "errors"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    MaxUploadSize = 10 << 20 // 10 MB
    UploadsDir    = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit the size of the request body to prevent large uploads
    r.Body = http.MaxBytesReader(w, r.Body, MaxUploadSize)

    // Parse multipart form (limits in ParseMultipartForm are advisory when MaxBytesReader is in place)
    if err := r.ParseMultipartForm(MaxUploadSize); err != nil {
        http.Error(w, "failed to parse multipart form", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing 'pdf' form file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Basic filename sanitization (do not use original filename directly for storage)
    origFilename := filepath.Base(header.Filename)
    if origFilename == "." || origFilename == "/" || origFilename == "" {
        http.Error(w, "invalid filename", http.StatusBadRequest)
        return
    }

    // Validate extension early (case-insensitive)
    if !strings.EqualFold(filepath.Ext(origFilename), ".pdf") {
        http.Error(w, "file must have .pdf extension", http.StatusBadRequest)
        return
    }

    // Ensure uploads directory exists with safe permissions
    if err := os.MkdirAll(UploadsDir, 0o755); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Peek into the beginning of the uploaded file to validate PDF magic bytes
    bufReader := bufio.NewReader(file)
    peek, err := bufReader.Peek(5) // PDF files start with "%PDF-"
    if err != nil && !errors.Is(err, io.EOF) {
        http.Error(w, "failed to read uploaded file", http.StatusBadRequest)
        return
    }
    if !bytes.HasPrefix(peek, []byte("%PDF")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Create a safe temporary file in uploads dir (os.CreateTemp ensures unique filename)
    dstFile, err := os.CreateTemp(UploadsDir, "upload-*.pdf")
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer func() {
        _ = dstFile.Close()
    }()

    // Copy the file contents to the destination with an additional size limit as a safety net
    limitedReader := io.LimitReader(bufReader, MaxUploadSize)
    if _, err := io.Copy(dstFile, limitedReader); err != nil {
        // If the copy failed, attempt to remove the partial file
        _ = os.Remove(dstFile.Name())
        http.Error(w, "failed to save uploaded file", http.StatusInternalServerError)
        return
    }

    // Set safe permissions on the stored file
    _ = dstFile.Chmod(0o644)

    // Respond with JSON containing only the generated filename (no paths, no sensitive info)
    resp := map[string]string{
        "status":   "ok",
        "filename": filepath.Base(dstFile.Name()),
        "original": origFilename, // original name returned only as info; not used for storage
    }
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    _ = json.NewEncoder(w).Encode(resp)
}
// ...existing code...
