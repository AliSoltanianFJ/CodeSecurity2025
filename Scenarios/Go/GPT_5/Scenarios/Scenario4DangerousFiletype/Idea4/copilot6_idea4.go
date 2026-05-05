package main

import (
    "bytes"
    "crypto/rand"
    "encoding/hex"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

const maxUploadSize = 10 << 20 // 10 MiB
const uploadDir = "./uploads"

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    if err := r.ParseMultipartForm(64 << 10); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing file", http.StatusBadRequest)
        return
    }
    defer file.Close()

    sniff := make([]byte, 512)
    n, err := io.ReadFull(file, sniff)
    if err != nil && err != io.ErrUnexpectedEOF && err != io.EOF {
        http.Error(w, "invalid file", http.StatusBadRequest)
        return
    }
    kind := http.DetectContentType(sniff[:n])
    if kind != "application/pdf" {
        http.Error(w, "unsupported file type", http.StatusBadRequest)
        return
    }

    cleanBase := sanitizeFilename(filepath.Base(header.Filename))
    if cleanBase == "" {
        cleanBase = "file"
    }
    if len(cleanBase) > 100 {
        cleanBase = cleanBase[:100]
    }

    ts := time.Now().UTC().Format("20060102T150405")
    randBytes := make([]byte, 6)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    suffix := hex.EncodeToString(randBytes)
    filename := ts + "-" + cleanBase + "-" + suffix + ".pdf"

    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    outPath := filepath.Join(uploadDir, filename)
    out, err := os.OpenFile(outPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0o644)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    reader := io.MultiReader(bytes.NewReader(sniff[:n]), file)
    if _, err := io.Copy(out, reader); err != nil {
        // attempt to remove partial file
        _ = out.Close()
        _ = os.Remove(outPath)
        http.Error(w, "unable to save file", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("uploaded"))
}

func sanitizeFilename(name string) string {
    name = strings.TrimSpace(name)
    // Remove directory components, ensure no leading dots
    name = filepath.Base(name)
    name = strings.TrimLeft(name, ".")
    var b strings.Builder
    for _, r := range name {
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') || r == '-' || r == '_' || r == '.' {
            b.WriteRune(r)
        } else {
            b.WriteRune('_')
        }
    }
    return b.String()
}
