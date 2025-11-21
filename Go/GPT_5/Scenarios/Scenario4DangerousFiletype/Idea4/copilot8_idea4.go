// ...existing code...
package main

import (
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

const maxUploadSize = 10 << 20 // 10 MB
const uploadDir = "./uploads"

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    defer file.Close()

    if header.Size > maxUploadSize {
        http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
        return
    }

    ext := strings.ToLower(filepath.Ext(header.Filename))
    if ext != ".pdf" {
        http.Error(w, "invalid file type", http.StatusBadRequest)
        return
    }

    sniff := make([]byte, 512)
    n, err := file.Read(sniff)
    if err != nil && err != io.EOF {
        http.Error(w, "unable to read file", http.StatusBadRequest)
        return
    }
    if n < 4 || !strings.HasPrefix(string(sniff[:n]), "%PDF") {
        http.Error(w, "invalid pdf", http.StatusBadRequest)
        return
    }

    if err := os.MkdirAll(uploadDir, 0700); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    randBytes := make([]byte, 16)
    if _, err := rand.Read(randBytes); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    filename := hex.EncodeToString(randBytes) + ".pdf"
    path := filepath.Join(uploadDir, filename)

    out, err := os.OpenFile(path, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer out.Close()

    if _, err := out.Write(sniff[:n]); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(out, file); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario4DangerousFiletype\Scenario4.go
// ...existing code...
package main

import (
    "crypto/rand"
    "encoding/hex"
    "io"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)
