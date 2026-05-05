// ...existing code...
package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
    "path/filepath"
    "strings"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit size to 10 MB
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // sanitize filename
    filename := filepath.Base(header.Filename)
    if strings.ToLower(filepath.Ext(filename)) != ".pdf" {
        http.Error(w, "only .pdf files are allowed", http.StatusBadRequest)
        return
    }

    // ensure upload directory exists
    uploadDir := "./uploads"
    if err := os.MkdirAll(uploadDir, 0755); err != nil {
        http.Error(w, "failed to create upload dir: "+err.Error(), http.StatusInternalServerError)
        return
    }

    dstPath := filepath.Join(uploadDir, filename)

    // read first bytes to validate content type, then write them plus the rest to destination
    head := make([]byte, 512)
    n, _ := file.Read(head)

    // basic content-type check
    if ct := http.DetectContentType(head[:n]); ct != "application/pdf" {
        // reject if detectable type isn't application/pdf
        http.Error(w, "uploaded file is not a valid pdf (content-type: "+ct+")", http.StatusBadRequest)
        return
    }

    dst, err := os.Create(dstPath)
    if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the bytes we already read, then copy the remainder
    if n > 0 {
        if _, err := dst.Write(head[:n]); err != nil {
            http.Error(w, "failed writing file: "+err.Error(), http.StatusInternalServerError)
            return
        }
    }
    if _, err := io.Copy(dst, file); err != nil {
        http.Error(w, "failed writing file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "file uploaded")
}
// ...existing code...
