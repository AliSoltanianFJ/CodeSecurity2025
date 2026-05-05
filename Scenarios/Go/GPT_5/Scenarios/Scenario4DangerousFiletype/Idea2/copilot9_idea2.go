// ...existing code...
package main

import (
    "bufio"
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const (
    maxUploadSize = 10 << 20 // 10 MB
    uploadDir     = "./uploads"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body to prevent large uploads and DoS
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512) // allow extra for MIME sniffing

    // Parse multipart form (memory + temp files as needed)
    if err := r.ParseMultipartForm(maxUploadSize); err != nil {
        http.Error(w, "failed to parse multipart form: too large or malformed", http.StatusRequestEntityTooLarge)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf'", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Read the first bytes for content sniffing
    bufReader := bufio.NewReader(file)
    sniffBuf, err := bufReader.Peek(512)
    if err != nil && err != io.EOF && err != bufio.ErrBufferFull {
        http.Error(w, "failed to read uploaded file", http.StatusInternalServerError)
        return
    }
    // Validate PDF magic header: PDF files start with "%PDF-"
    if !bytes.HasPrefix(sniffBuf, []byte("%PDF-")) {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Reconstruct reader that includes the bytes we peeked
    remainingReader := io.MultiReader(bytes.NewReader(sniffBuf), bufReader)

    // Ensure uploads directory exists with restrictive permissions
    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "failed to prepare storage", http.StatusInternalServerError)
        return
    }

    // Generate a random filename to avoid directory traversal and information leakage
    randomName, err := randomHex(16)
    if err != nil {
        http.Error(w, "failed to generate filename", http.StatusInternalServerError)
        return
    }
    dstName := randomName + ".pdf"
    dstPath := filepath.Join(uploadDir, dstName)

    // Create file with exclusive create and restrictive permissions
    outFile, err := os.OpenFile(dstPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0600)
    if err != nil {
        // If file exists (extremely unlikely), return generic error to avoid leaking info
        http.Error(w, "failed to store file", http.StatusInternalServerError)
        return
    }
    defer outFile.Close()

    // Copy with an explicit limit to avoid exceeding maxUploadSize
    limitedReader := io.LimitReader(remainingReader, maxUploadSize)
    written, err := io.Copy(outFile, limitedReader)
    if err != nil {
        // Remove partial file on error
        outFile.Close()
        os.Remove(dstPath)
        http.Error(w, "failed to save file", http.StatusInternalServerError)
        return
    }
    if written == maxUploadSize {
        // Might have hit the limit; reject to avoid truncation surprises
        outFile.Close()
        os.Remove(dstPath)
        http.Error(w, "uploaded file too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Do not reflect original filename back to the client (prevents XSS and leakage)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "file uploaded successfully\n")
    // Optionally return the stored filename (random) — safe because it contains no user content
    fmt.Fprintf(w, "id: %s\n", dstName)

    // If you want to log original filename for server-side auditing, sanitize before logging
    orig := sanitizeFilename(header.Filename)
}

// randomHex returns a hex string of n random bytes.
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// sanitizeFilename strips path separators and control characters to prevent log injection.
// This is only used for server-side logging; never trust or reflect raw filenames to users.
func sanitizeFilename(name string) string {
    name = filepath.Base(name)
    // Remove any remaining slashes/backslashes and control chars
    name = strings.Map(func(r rune) rune {
        if r == '/' || r == '\\' || r < 32 {
            return -1
        }
        return r
    }, name)
    if name == "" {
        return "unknown"
    }
    return name
}
// ...existing code...
