package main

import (
    "crypto/rand"
    "encoding/hex"
    "encoding/json"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    uploadDir      = "./uploads"
    maxUploadBytes = 10 << 20 // 10 MB
)

type jsonResp struct {
    Error  string `json:"error,omitempty"`
    Status string `json:"status,omitempty"`
}

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Always respond with JSON and a safe content type
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Only accept POST
    if r.Method != http.MethodPost {
        w.WriteHeader(http.StatusMethodNotAllowed)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "method not allowed"})
        return
    }

    // Limit total request size to prevent large uploads (defends against resource exhaustion)
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadBytes)

    // Parse the multipart form. The maxMemory is small because body is already limited.
    if err := r.ParseMultipartForm(32 << 10); err != nil {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "invalid multipart form or body too large"})
        return
    }
    // Clean up temporary files (if any)
    if r.MultipartForm != nil {
        defer r.MultipartForm.RemoveAll()
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "missing file field 'pdf'"})
        return
    }
    defer file.Close()

    // Sanitize filename input (do not trust it). Use only base name and lower-case extension check.
    origName := filepath.Base(header.Filename)
    ext := strings.ToLower(filepath.Ext(origName))
    if ext != ".pdf" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "file must have .pdf extension"})
        return
    }

    // Read first bytes to verify PDF magic "%PDF-"
    peek := make([]byte, 512)
    n, err := file.Read(peek)
    if err != nil && err != io.EOF {
        w.WriteHeader(http.StatusInternalServerError)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "failed reading upload"})
        return
    }
    if !isPDF(peek[:n]) {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "uploaded file is not a valid PDF"})
        return
    }

    // Ensure uploads directory exists with restrictive permissions
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "failed to create upload directory"})
        return
    }

    // Generate a safe random filename to avoid collisions and path traversal
    targetName, err := randomFilename(".pdf")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "failed to generate filename"})
        return
    }
    targetPath := filepath.Join(uploadDir, targetName)

    // Use O_EXCL to avoid following symlinks or overwriting files
    dst, err := os.OpenFile(targetPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "failed to create destination file"})
        return
    }
    defer func() {
        _ = dst.Close()
    }()

    // Write initial bytes already read, then copy the rest. Protect overall size.
    if _, err := dst.Write(peek[:n]); err != nil {
        _ = os.Remove(targetPath)
        w.WriteHeader(http.StatusInternalServerError)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "failed to write file"})
        return
    }

    // Calculate remaining allowed bytes (defensive). maxUploadBytes refers to body; subtract what we've already consumed.
    remaining := int64(maxUploadBytes) - int64(n)
    if remaining < 0 {
        _ = os.Remove(targetPath)
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "file too large"})
        return
    }

    // Copy the rest but limit to 'remaining' bytes.
    written, err := io.Copy(dst, io.LimitReader(file, remaining))
    if err != nil {
        _ = os.Remove(targetPath)
        w.WriteHeader(http.StatusInternalServerError)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "failed while saving file"})
        return
    }

    // If client attempted to send more than remaining, reject and remove the file.
    if written == remaining {
        // There may be more data — refuse to accept oversized file.
        _ = os.Remove(targetPath)
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "file too large"})
        return
    }

    // Success: return safe confirmation (do not echo user-provided filename or any internal paths)
    w.WriteHeader(http.StatusCreated)
    _ = json.NewEncoder(w).Encode(jsonResp{Status: "uploaded"})
}

// isPDF checks PDF header magic. Simple, reliable initial check.
func isPDF(b []byte) bool {
    if len(b) < 4 {
        return false
    }
    // PDF files start with "%PDF-" (percent sign, 'P','D','F','-')
    return string(b[:5]) == "%PDF-"
}

// randomFilename returns a secure random hex filename with the given extension.
func randomFilename(ext string) (string, error) {
    r := make([]byte, 16) // 128-bit random
    if _, err := rand.Read(r); err != nil {
        return "", err
    }
    return hex.EncodeToString(r) + ext, nil
}
