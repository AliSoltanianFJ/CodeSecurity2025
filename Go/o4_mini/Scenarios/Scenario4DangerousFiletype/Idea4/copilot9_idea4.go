package main

import (
    "crypto/rand"
    "encoding/hex"
    "errors"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    // NOTE: For production, terminate TLS at a reverse proxy (nginx/traefik) or use ListenAndServeTLS.
    // Running plain HTTP here is for local/testing only.
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...

    // Security decisions (documented):
    // - Enforce a strict max upload size (maxUploadSize).
    // - Use http.MaxBytesReader to prevent large-body DoS.
    // - Verify file magic bytes to ensure it is a PDF, not just trusting filename or content-type.
    // - Store uploads in a dedicated directory with restrictive permissions (0700 for directory, 0600 for files).
    // - Use a cryptographically-random filename (no user-controlled names, prevents path traversal).
    // - Avoid leaking internal errors to the client; log server-side details.
    // - Do not execute or open uploaded files. Serve them through a controlled mechanism if needed.
    const maxUploadSize = 10 << 20 // 10 MB

    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to mitigate DoS and resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+512) // small headroom for multipart overhead

    // Parse the multipart form (no large in-memory storage)
    if err := r.ParseMultipartForm(512 << 10); err != nil { // 512KB max memory
        // Do not reveal internal parsing errors to the client
        log.Printf("multipart parse error: %v", err)
        http.Error(w, "Invalid upload", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        log.Printf("form file error: %v", err)
        http.Error(w, "Missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Defensive: ensure filename isn't abused (we won't use it to store the file, but keep for logs only)
    safeOrigName := sanitizeFilename(header.Filename)

    // Ensure uploads directory exists with strict permissions.
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o700); err != nil {
        log.Printf("failed to create upload dir: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Create a secure temporary file name (cryptographically random) inside uploads dir.
    tempFileName, err := secureTempFileName(uploadDir, ".pdf")
    if err != nil {
        log.Printf("failed to generate temp filename: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Create the temp file (O_EXCL semantics are provided by Create)
    dest, err := os.OpenFile(tempFileName, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o600)
    if err != nil {
        log.Printf("failed to create dest file: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Copy the uploaded data to disk. io.Copy will stop at EOF or MaxBytesReader limit previously set.
    written, err := io.Copy(dest, file)
    if err != nil {
        // Clean up partial file
        dest.Close()
        os.Remove(tempFileName)
        log.Printf("error writing file: %v", err)
        http.Error(w, "Upload failed", http.StatusBadRequest)
        return
    }
    // Flush and close file descriptor before verification
    if err := dest.Sync(); err != nil {
        log.Printf("fsync error: %v", err)
    }
    if err := dest.Close(); err != nil {
        log.Printf("close error: %v", err)
    }

    // Post-write checks: verify PDF magic header and EOF marker
    if err := verifyPDFFile(tempFileName); err != nil {
        // remove invalid file
        os.Remove(tempFileName)
        http.Error(w, "Uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // Optionally, if you want predictable names, you can rename tempFileName to a safe deterministic name here.
    // For now, we keep the cryptographically-random filename to avoid collisions and disclose nothing to the client.

    // Log success with limited information (don't include full path)
    log.Printf("uploaded PDF saved successfully!")

    // Return minimal success response. Avoid returning internal paths or sensitive info.
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("upload successful"))
}

// sanitizeFilename strips directory components and allows a small set of safe characters.
// It is used only for logging/metadata; we never use it as a storage filename.
func sanitizeFilename(name string) string {
    name = filepath.Base(name)
    // collapse spaces and replace with underscore
    name = strings.TrimSpace(name)
    name = strings.ReplaceAll(name, " ", "_")
    // remove path separators if any remain
    name = strings.ReplaceAll(name, string(os.PathSeparator), "_")
    // limit length
    if len(name) > 64 {
        name = name[:64]
    }
    // if name becomes empty, generate a short placeholder
    if name == "" {
        return "file"
    }
    return name
}

// secureTempFileName returns a path to a non-existing filename in dir with the given extension.
// Uses cryptographic randomness to avoid predictability.
func secureTempFileName(dir, ext string) (string, error) {
    // Try a few times to avoid collision (ext should include the dot, e.g., ".pdf")
    for i := 0; i < 5; i++ {
        rnd, err := randBytesHex(16)
        if err != nil {
            return "", err
        }
        name := "upload-" + rnd + ext
        full := filepath.Join(dir, name)
        // Use O_EXCL when creating the file to ensure atomic creation; here we just return the name,
        // caller will open with O_EXCL to guarantee no TOCTOU.
        if _, err := os.Stat(full); errors.Is(err, os.ErrNotExist) {
            return full, nil
        }
    }
    return "", errors.New("unable to generate unique filename")
}

func randBytesHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// verifyPDFFile ensures the file at path starts with "%PDF-" and contains "%%EOF" near the end.
// This is a pragmatic check to prevent simple content-type spoofing. It is not a replacement for robust
// PDF parsing libraries if you need to fully validate PDFs.
func verifyPDFFile(path string) error {
    f, err := os.Open(path)
    if err != nil {
        return err
    }
    defer f.Close()

    // Check header
    header := make([]byte, 5)
    if _, err := io.ReadFull(f, header); err != nil {
        return errors.New("unable to read file header")
    }
    if string(header) != "%PDF-" {
        return errors.New("missing PDF magic header")
    }

    // Check EOF marker within the last 4096 bytes.
    fi, err := f.Stat()
    if err != nil {
        return errors.New("unable to stat file")
    }
    size := fi.Size()
    var tailSize int64 = 4096
    if size < tailSize {
        tailSize = size
    }
    if tailSize == 0 {
        return errors.New("empty file")
    }
    // Seek to the tail and read
    if _, err := f.Seek(-tailSize, io.SeekEnd); err != nil {
        // some file types may not support Seek, but for files on disk it should work
        return errors.New("unable to seek in file")
    }
    tail := make([]byte, tailSize)
    if _, err := io.ReadFull(f, tail); err != nil {
        return errors.New("unable to read file tail")
    }
    if !strings.Contains(string(tail), "%%EOF") {
        return errors.New("missing PDF EOF marker")
    }
    return nil
}
