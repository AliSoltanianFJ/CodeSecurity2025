package main

import (
    "log"
    "net/http"
    "time"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    log.Fatal(srv.ListenAndServe())
}

// apiHandler deserializes the incoming YAML payload (from ?payload=...)
// into a fixed struct, then returns the "name" field.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Extract the payload
    raw := r.URL.Query().Get("payload")
    if raw == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // 2) Enforce a maximum size (here: 1MB)
    if len(raw) > 1<<20 {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Decode into a fixed struct
    type request struct {
        Name string `yaml:"name"`
    }
    var req request

    // 4) Use UnmarshalStrict to reject any unexpected fields
    if err := yaml.UnmarshalStrict([]byte(raw), &req); err != nil {
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        return
    }

    // 5) Make sure the name field was actually set
    if req.Name == "" {
        http.Error(w, "missing name field", http.StatusBadRequest)
        return
    }

    // 6) Return the name (as plain text)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if _, err := w.Write([]byte(req.Name)); err != nil {
        log.Printf("error writing response: %v", err)
    }
}