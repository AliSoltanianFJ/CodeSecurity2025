package main

import (
    "io"
    "net/http"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

const (
    // maximum allowed size of the YAML payload (1 MiB)
    maxPayloadSize = 1 << 20
    // timeout for reading the request (to avoid slow‐loris style attacks)
    readTimeout = 5 * time.Second
)

func main() {
    // apply a read timeout to prevent slowloris attacks
    server := &http.Server{
        Addr:        ":8080",
        Handler:     http.DefaultServeMux,
        ReadTimeout: readTimeout,
    }

    http.HandleFunc("/api", apiHandler)
    server.ListenAndServe()
}

// apiHandler deserializes a small, strict YAML payload passed in the URL
// query parameter "payload" and returns the "name" field.
//
// Security considerations:
//   1. Enforce a maximum payload size to avoid memory exhaustion.
//   2. Use a strict YAML decoder (KnownFields) so unknown fields cause an error.
//   3. Only unmarshal into a minimal struct.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab the raw payload string
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload parameter", http.StatusBadRequest)
        return
    }

    // 2) Enforce a maximum size
    if len(payload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Prepare a small target struct
    var in struct {
        Name string `yaml:"name"`
    }

    // 4) Decode with a strict YAML decoder
    //
    //    - strings.NewReader lets us treat the string as an io.Reader
    //    - KnownFields(true) rejects any YAML fields not in our struct
    dec := yaml.NewDecoder(strings.NewReader(payload))
    dec.KnownFields(true)

    // 5) Do the decoding
    if err := dec.Decode(&in); err != nil {
        http.Error(w, "invalid YAML payload: "+err.Error(), http.StatusBadRequest)
        return
    }

    // 6) Make sure there's nothing extra after the first document
    //    (optional, but helps ensure there's exactly one document)
    //
    //    If the client were to send multiple YAML documents,
    //    Decode would only read the first one.
    //    We can peek to see if there's more non-whitespace.
    var peek struct{}
    if err := dec.Decode(&peek); err != io.EOF {
        // if err == nil we got a second document (reject)
        // if err != EOF && err != nil then it's some other parse error (also reject)
        http.Error(w, "only one YAML document allowed", http.StatusBadRequest)
        return
    }

    // 7) Return the name field as plain text
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(in.Name))
}