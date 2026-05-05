package main

import (
    "encoding/json"
    "net/http"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    // It’s always a good idea to set a reasonable read timeout so that
    // a slow or malicious client can’t tie up your goroutines forever.
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
    }
    srv.ListenAndServe()
}

// apiHandler pulls a URL‐query “payload”, does some basic checks,
// unmarshals only into a struct with a single field, and returns that field.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab the payload from the URL.
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload parameter", http.StatusBadRequest)
        return
    }

    // 2) Enforce an absolute maximum size for the incoming YAML.
    //    Since you’re accepting it via URL, this should be small anyway,
    //    but it’s a good habit to avoid giant input attacks.
    const maxPayloadLen = 4096
    if len(payload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Reject any YAML that attempts to use anchors or aliases.
    //    These can be used to mount “billion-laughs” style attacks.
    if strings.ContainsAny(payload, "&*") {
        http.Error(w, "anchors and aliases not allowed", http.StatusBadRequest)
        return
    }

    // 4) Define exactly what structure we expect.  No maps, no interfaces.
    type requestPayload struct {
        Name string `yaml:"name"`
    }
    var req requestPayload

    // 5) Unmarshal.  Because we’re using a concrete struct, there’s no
    //    chance of arbitrary objects or code execution.  If the input
    //    doesn’t match exactly, you get an error.
    if err := yaml.Unmarshal([]byte(payload), &req); err != nil {
        http.Error(w, "invalid YAML: "+err.Error(), http.StatusBadRequest)
        return
    }

    // 6) Make sure we actually got a name back.
    if req.Name == "" {
        http.Error(w, "`name` field is required", http.StatusBadRequest)
        return
    }

    // 7) Return it as the minimal JSON response.
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]string{
        "name": req.Name,
    })
}