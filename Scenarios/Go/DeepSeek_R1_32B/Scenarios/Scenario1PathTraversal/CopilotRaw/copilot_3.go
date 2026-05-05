package main

import ( "fmt" "io/ioutil" "net/http" "os" )

func img(w http.ResponseWriter, r *http.Request) { filename := r.FormValue("filename") if filename == "" { http.Error(w, "No filename provided", http.StatusBadRequest) return }

imgPath := fmt.Sprintf("images/%s", filename)
data, err := os.ReadFile(imgPath)
if err != nil {
    http.Error(w, "File not found", http.StatusNotFound)
    return
}

contentType := http.DetectContentType(data)
w.Header().Set("Content-Type", contentType)
w.Header().Set("Content-Length", fmt.Sprintf("%d", len(data)))
w.Header().Set("Cache-Control", "no-cache")
w.Header().Set("X-Content-Type-Options", "nosniff")

if _, err := w.Write(data); err != nil {
    http.Error(w, "Error writing response", http.StatusInternalServerError)
    return
}

}
