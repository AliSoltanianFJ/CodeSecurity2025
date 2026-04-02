package main

import (
    "html"
    "io"
    "log"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// apiHandler reads a single‐document YAML payload from the POST body,
// extracts only the "name" field, and returns it.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Enforce a sane content‐type (optional, but helps catch mistakes)
    if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "application/x-yaml") {
        http.Error(w, "invalid content type, want application/x-yaml", http.StatusBadRequest)
        return
    }

    // 3) Limit the size of the incoming body to prevent OOM attacks
    const maxBodySize = 1 << 10 // 1 KiB
    r.Body = http.MaxBytesReader(w, r.Body, maxBodySize)
    defer r.Body.Close()

    // 4) Define a struct that captures exactly the fields we want
    var payload struct {
        Name string `yaml:"name"`
    }

    // 5) Decode with KnownFields(true) to disallow any extraneous fields
    dec := yaml.NewDecoder(r.Body)
    dec.KnownFields(true)

    if err := dec.Decode(&payload); err != nil {
        http.Error(w, "bad yaml payload: "+err.Error(), http.StatusBadRequest)
        return
    }

    // 6) Reject multi-document YAML
    //    (a second document would turn into an empty struct{} or another YAML doc)
    var trailer struct{}
    if err := dec.Decode(&trailer); err != io.EOF {
        http.Error(w, "multiple YAML documents not allowed", http.StatusBadRequest)
        return
    }

    // 7) Escape the name to prevent reflected XSS if we ever switch to HTML/JS output
    safeName := html.EscapeString(payload.Name)

    // 8) Return the name
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte(safeName))
}