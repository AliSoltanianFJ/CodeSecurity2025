package main

import (
    "fmt"
    "io"
    "net/http"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

const (
    // we don't expect huge payloads in a URL param:
    maxPayloadSize = 1 << 20 // 1 MiB
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

// apiHandler extracts the `payload` query parameter, treats it as YAML,
// decodes it into a small concrete struct, and returns the "name" field.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Extract the raw payload
    raw := r.URL.Query().Get("payload")
    if raw == "" {
        http.Error(w, "`payload` query parameter is required", http.StatusBadRequest)
        return
    }

    // 2) Enforce a hard size limit so we don't OOM or DoS ourselves
    if len(raw) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Prepare a YAML decoder over the string
    reader := strings.NewReader(raw)
    dec := yaml.NewDecoder(reader)

    // Optional: cap alias expansion (guards against "billion laughs" etc.)
    // You can tune this limit as you see fit.
    dec.SetMaxAlias(50)

    // Enforce that only known fields in our struct are allowed
    dec.KnownFields(true)

    // 4) Decode into a concrete struct
    var in struct {
        Name string `yaml:"name"`
    }
    if err := dec.Decode(&in); err != nil && err != io.EOF {
        http.Error(w, fmt.Sprintf("invalid YAML: %v", err), http.StatusBadRequest)
        return
    }

    // 5) If you expect exactly one document, you can prevent
    //    extra data by checking for a second document:
    if err := dec.Decode(new(interface{})); err != io.EOF {
        http.Error(w, "only a single YAML document is allowed", http.StatusBadRequest)
        return
    }

    // 6) Return the result
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    fmt.Fprint(w, in.Name)
}