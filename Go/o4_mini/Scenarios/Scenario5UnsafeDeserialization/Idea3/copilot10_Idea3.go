package main

import (
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

func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET (or you could switch this to POST and read from the body)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Grab the raw YAML from the “payload” query-string
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload parameter", http.StatusBadRequest)
        return
    }

    // 3) Enforce a sane maximum length
    const maxPayloadSize = 1024 // 1 KiB
    if len(payload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 4) Define exactly what fields we expect
    type requestYAML struct {
        Name string `yaml:"name"`
    }
    var req requestYAML

    // 5) Unmarshal
    if err := yaml.Unmarshal([]byte(payload), &req); err != nil {
        http.Error(w, "invalid YAML", http.StatusBadRequest)
        return
    }

    // 6) Name is required
    req.Name = strings.TrimSpace(req.Name)
    if req.Name == "" {
        http.Error(w, "missing or empty name field", http.StatusBadRequest)
        return
    }

    // 7) Return just the name, as plain text
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    if _, err := w.Write([]byte(req.Name)); err != nil {
        // this is just logging: the response is already headed out
        log.Printf("error writing response: %v", err)
    }
}