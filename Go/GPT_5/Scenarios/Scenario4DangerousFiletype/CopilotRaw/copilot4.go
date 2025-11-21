// ...existing code...
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

// ...existing code...
/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "failed to read form file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // Peek at the first bytes to validate PDF magic ("%PDF")
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed to read uploaded file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if n < 4 || !bytes.HasPrefix(buf[:n], []byte("%PDF")) {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    uploadsDir := "./uploads"
    if err := os.MkdirAll(uploadsDir, 0755); err != nil {
        http.Error(w, "failed to create uploads directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // Sanitize filename to avoid path traversal and empty names
    filename := filepath.Base(header.Filename)
    if filename == "" || filename == "." {
        filename = fmt.Sprintf("upload-%d.pdf", time.Now().UnixNano())
    }

    destPath := filepath.Join(uploadsDir, filename)

    // Create destination file without overwriting existing files
    dest, err := os.OpenFile(destPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0644)
    if os.IsExist(err) {
        base := strings.TrimSuffix(filename, filepath.Ext(filename))
        ext := filepath.Ext(filename)
        for i := 1; ; i++ {
            tryName := fmt.Sprintf("%s-%d%s", base, i, ext)
            destPath = filepath.Join(uploadsDir, tryName)
            dest, err = os.OpenFile(destPath, os.O_CREATE|os.O_WRONLY|os.O_EXCL, 0644)
            if err == nil {
                break
            }
            if !os.IsExist(err) {
                http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
                return
            }
        }
    } else if err != nil {
        http.Error(w, "failed to create destination file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dest.Close()

    // Write the bytes already read, then copy the rest
    if _, err := dest.Write(buf[:n]); err != nil {
        http.Error(w, "failed to write file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if _, err := io.Copy(dest, file); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "uploaded file saved as %s", destPath)
}
// ...existing code...
