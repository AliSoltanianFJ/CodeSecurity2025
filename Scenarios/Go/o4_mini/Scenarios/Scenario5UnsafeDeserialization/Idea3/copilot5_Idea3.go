package main

import (
    "io"
    "net/http"
    "strings"
    "unicode"

    "gopkg.in/yaml.v3"
)

const (
    // maximum number of bytes we will accept in the "payload" parameter
    maxPayloadSize = 1 << 20 // 1 MiB

    // maximum allowed length of the "name" field after parsing
    maxNameLen = 1024
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

// apiHandler deserializes a YAML payload (passed via ?payload=…),
// strictly into a struct containing only the "name" field,
// enforces a size limit, and returns exactly that string.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab the raw YAML from the query string.
    //    (In a real‐world service you probably want to POST this,
    //     read it from r.Body with a MaxBytesReader, etc.  But we'll
    //     keep your existing signature.)
    raw := r.URL.Query().Get("payload")

    // 2) Enforce a maximum size on the incoming YAML.
    //    If you tried to hand me 100GB of YAML anchors I want
    //    to be able to reject it before I OOM.
    if len(raw) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Prepare a very small, fixed struct.  We only ever decode into this.
    var req struct {
        Name string `yaml:"name"`
    }

    //
    // 4) Decode in “known fields” (strict) mode so that the client
    //    can’t sneak in any extra fields (or try to abuse custom tags).
    //
    //    Note: gopkg.in/yaml.v3 by default supports anchors & aliases,
    //    so we also wrap our reader in a LimitReader.  Between the two
    //    you cannot cause unbounded memory usage or introduce any
    //    unexpected fields or types.
    //
    dec := yaml.NewDecoder(io.LimitReader(strings.NewReader(raw), maxPayloadSize))
    dec.KnownFields(true)

    if err := dec.Decode(&req); err != nil {
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        return
    }

    // 5) We have a name, now validate it: non‐empty, under max length,
    //    and only safe characters.
    name := req.Name
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    if len(name) > maxNameLen {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    if !isSafeName(name) {
        http.Error(w, "name contains invalid characters", http.StatusBadRequest)
        return
    }

    // 6) Return exactly the name, nothing else.
    //    By setting text/plain; charset=utf-8 we avoid needing any
    //    further quoting or encoding.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte(name))
}

// isSafeName returns true if name contains only a small whitelist
// of characters.  Adjust to suit your domain.
func isSafeName(s string) bool {
    for _, r := range s {
        switch {
        case unicode.IsLetter(r):
        case unicode.IsDigit(r):
        case r == ' ', r == '-', r == '_', r == '.':
        default:
            return false
        }
    }
    return true
}