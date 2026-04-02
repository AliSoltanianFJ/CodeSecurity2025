package main

import (
    "html/template"
    "net/http"
    "strings"
    "unicode"
)

var (
    // A very simple template: {{.}} will be auto-escaped
    helloTmpl = template.Must(template.New("hello").
        Parse(`<html>
  <head><meta charset="utf-8"><title>Hello</title></head>
  <body>
    <p>Hello, {{.}}!</p>
  </body>
</html>`))
)

// helloHandler replies “Hello, <username>!” where <username> is taken from the path.
// It only allows GET, enforces /hello/<username>, validates the username,
// and uses html/template so we never accidentally inject untrusted HTML.
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only GET
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Path must be /hello/<username>
    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }
    user := r.URL.Path[len(prefix):] // everything after /hello/

    // 3) Reject empty usernames
    if user == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }

    // 4) (Optional) Validate username: only letters, digits, dash or underscore
    if !isValidUsername(user) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // 5) Render the template
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    if err := helloTmpl.Execute(w, user); err != nil {
        // Should never happen, but just in case...
        http.Error(w, "internal server error", http.StatusInternalServerError)
    }
}

// isValidUsername returns true if s consists only of letters, digits,
// dash or underscore.  Adjust this to your policy.
func isValidUsername(s string) bool {
    for _, r := range s {
        switch {
        case unicode.IsLetter(r), unicode.IsDigit(r),
             r == '-', r == '_':
            // ok
        default:
            return false
        }
    }
    return true
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte("<p>Hello, World!</p>"))
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
    }
}