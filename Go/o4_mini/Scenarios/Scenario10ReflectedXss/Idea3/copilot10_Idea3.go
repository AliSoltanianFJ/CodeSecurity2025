package main

import (
    "html/template"
    "net/http"
    "net/url"
    "path"
    "regexp"
    "strings"
)

var (
    // pre‐compile the username validation regex
    validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)

    // pre‐parse our small template
    helloTmpl = template.Must(template.
        New("hello").
        Parse(`<html>
  <head><meta charset="utf-8"><title>Hello</title></head>
  <body>
    <p>Hello, {{.Username}}!</p>
  </body>
</html>`))
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte("<p>Hello, World!</p>"))
}

// helloHandler says “Hello, <username>!” at /hello/<username>
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only GET
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Clean the URL path (removes .., extra slashes, trailing slash, etc.)
    //    If someone requested “/hello/Alice/”, path.Clean gives “/hello/Alice”
    p := path.Clean(r.URL.Path)

    // 3) Extract the “username” portion
    const prefix = "/hello/"
    if !strings.HasPrefix(p, prefix) {
        http.NotFound(w, r)
        return
    }
    raw := p[len(prefix):] // everything after “/hello/”

    if raw == "" {
        http.Error(w, "Username not provided", http.StatusBadRequest)
        return
    }

    // 4) Reject any remaining “/” in the raw segment
    if strings.Contains(raw, "/") {
        http.NotFound(w, r)
        return
    }

    // 5) URL‐unescape (so “%41lice” → “Alice”)
    user, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "Invalid username encoding", http.StatusBadRequest)
        return
    }

    // 6) Validate against our safe character set
    if !validUsername.MatchString(user) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 7) All done! Render the template.
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    err = helloTmpl.Execute(w, struct{ Username string }{Username: user})
    if err != nil {
        // If the template write fails for some reason, report 500
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
}