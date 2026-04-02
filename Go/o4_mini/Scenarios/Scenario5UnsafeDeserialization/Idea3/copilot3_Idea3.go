package main

import (
    "fmt"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)

    // It’s also a good idea in production to set Read/Write timeouts
    // on your server, but that’s outside the scope of this little handler.
    http.ListenAndServe(":8080", nil)
}

// apiHandler reads a YAML document from the "payload" query parameter,
// extracts only the "name" field, and returns it as plain text.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET (or POST if you prefer)—reject everything else.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Pull the payload out of the URL.  (Usually one uses the body,
    //    but the prompt requested a URL‐param.)
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // 3) Enforce a reasonable size limit.  Since this is a URL‐param
    //    it can’t be terribly huge, but better safe than sorry.
    const maxPayloadSize = 1 << 20 // 1 MiB
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 4) Decode ONLY into a struct that contains exactly what we want.
    //    This prevents the YAML decoder from ever instantiating
    //    arbitrary types or running any custom UnmarshalYAML logic.
    type request struct {
        Name string `yaml:"name"`
    }
    var req request

    // 5) Use yaml.NewDecoder + KnownFields(true) to force an error
    //    if the client sends anything other than `name: …`
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true)

    if err := decoder.Decode(&req); err != nil {
        http.Error(w, fmt.Sprintf("invalid YAML: %v", err), http.StatusBadRequest)
        return
    }

    // 6) Ensure we actually got a name
    if req.Name == "" {
        http.Error(w, "missing or empty 'name' field", http.StatusBadRequest)
        return
    }

    // 7) Return it as plain text.  No HTML, no JSON, so no XSS vector here.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(req.Name))
}