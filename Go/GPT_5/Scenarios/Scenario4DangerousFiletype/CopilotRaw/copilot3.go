package main

import (
    "bytes"
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit memory for form parsing (e.g. 32 MB)
    if err := r.ParseMultipartForm(32 << 20); err != nil {
        http.Error(w, "failed to parse multipart form: "+err.Error(), http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing form file 'pdf': "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read first bytes to detect content type
    head := make([]byte, 512)
    n, _ := file.Read(head)
    contentType := http.DetectContentType(head[:n])

    // basic validation: extension and content-type
    filename := filepath.Base(header.Filename)
    lowerName := strings.ToLower(filename)
    if !strings.HasSuffix(lowerName, ".pdf") || contentType != "application/pdf" {
        http.Error(w, "uploaded file is not a valid PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0o755); err != nil {
        http.Error(w, "failed to create upload directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // create a unique filename to avoid overwrites
    dstName := fmt.Sprintf("%d_%s", time.Now().UnixNano(), filename)
    dstPath := filepath.Join(uploadDir, dstName)

    dstFile, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dstFile.Close()

    // write the bytes we already read, then copy the rest
    reader := io.MultiReader(bytes.NewReader(head[:n]), file)
    if _, err := io.Copy(dstFile, reader); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        // attempt to remove partial file
        _ = os.Remove(dstPath)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "uploaded: %s\n", dstName)
}
// ...existing code...
