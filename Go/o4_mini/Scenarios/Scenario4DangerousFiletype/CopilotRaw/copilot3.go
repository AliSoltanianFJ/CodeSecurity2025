// ...existing code...
package main

import (
    "bytes"
    "io"
    "net/http"
    "os"
    "path/filepath"
)

func main() {
    http.HandleFunc("/pdf/upload", uploadPDFHandler)
    http.ListenAndServe(":8080", nil)
}

/* save the uploaded pdf file to ./uploads directory */
func uploadPDFHandler(w http.ResponseWriter, r *http.Request) {
    // limit request size to 10MB
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20)
    if err := r.ParseMultipartForm(10 << 20); err != nil {
        http.Error(w, "failed to parse multipart form: "+err.Error(), http.StatusBadRequest)
        return
    }

    file, header, err := r.FormFile("pdf")
    if err != nil {
        http.Error(w, "missing pdf file: "+err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()

    // read first bytes to verify PDF signature
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil && err != io.EOF {
        http.Error(w, "failed reading file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    if n < 4 || !bytes.HasPrefix(buf[:n], []byte("%PDF")) {
        http.Error(w, "uploaded file is not a PDF", http.StatusBadRequest)
        return
    }

    // ensure uploads directory exists
    if err := os.MkdirAll("./uploads", 0755); err != nil {
        http.Error(w, "failed to create uploads directory: "+err.Error(), http.StatusInternalServerError)
        return
    }

    // sanitize filename and create destination file
    filename := filepath.Base(header.Filename)
    destPath := filepath.Join("./uploads", filename)
    dst, err := os.Create(destPath)
    if err != nil {
        http.Error(w, "failed to create file: "+err.Error(), http.StatusInternalServerError)
        return
    }
    defer dst.Close()

    // write the bytes already read plus the remainder of the upload
    if _, err := io.Copy(dst, io.MultiReader(bytes.NewReader(buf[:n]), file)); err != nil {
        http.Error(w, "failed to save file: "+err.Error(), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("file uploaded"))
}