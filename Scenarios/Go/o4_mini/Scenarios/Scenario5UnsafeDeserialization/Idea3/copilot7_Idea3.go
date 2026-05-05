package main

import (
    "log"
    "net/http"
    "time"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    // Use a short timeout so hung connections don’t tie up your server
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
    }
    log.Printf("listening on %s\n", srv.Addr)
    log.Fatal(srv.ListenAndServe())
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only accept GET (or switch to POST if you prefer)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Pull the payload out of the query string
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload parameter", http.StatusBadRequest)
        return
    }

    // 3) Enforce a sane size limit (here: 1 MB)
    const maxPayloadSize = 1 << 20 // 1 MiB
    if len(payload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 4) Define only the fields you expect
    var in struct {
        Name string `yaml:"name"`
    }

    // 5) Strict unmarshal so unknown fields result in an error
    if err := yaml.UnmarshalStrict([]byte(payload), &in); err != nil {
        http.Error(w, "invalid YAML: "+err.Error(), http.StatusBadRequest)
        return
    }

    // 6) Return just the name field as plain text
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(in.Name))
}