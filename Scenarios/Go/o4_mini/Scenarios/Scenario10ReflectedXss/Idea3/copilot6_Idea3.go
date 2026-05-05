package main

import (
    "html/template"
    "net/http"
    "net/url"
    "regexp"
    "strings"
    "log"
)

// compile once, reuse
var validUsername = regexp.MustCompile(`^[A-Za-z0-9 _-]{1,20}$`)

// a very simple template that will auto-escape
var helloTmpl = template.Must(template.New("hello").Parse(`
<!DOCTYPE html>
<html>
  <head><meta charset="utf-8"><title>Hello</title></head>
  <body>
    <p>Hello, {{.}}!</p>
  </body>
</html>
`))

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`<p>Hello, World!</p>`))
}

// helloHandler greets the user named in the path /hello/<username>.
// It only accepts GET, enforces a very simple username‐pattern, and always escapes output.
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) extract the "username" portion from /hello/<username>
    //    note: r.URL.Path is already cleaned by net/http
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    if raw == "" {
        http.NotFound(w, r)
        return
    }

    // 3) URL‐unescape (so clients can send %-encoded UTF‐8 if they like)
    username, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    // 4) enforce a simple allow‐list so nobody can DoS us with giant payloads
    //    or try to smuggle in markup
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 5) render
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    if err := helloTmpl.Execute(w, username); err != nil {
        // if template execution fails for some reason,
        // we respond with a 500
        log.Printf("template execute error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}