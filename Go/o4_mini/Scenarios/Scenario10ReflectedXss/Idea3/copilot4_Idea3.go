package main

import (
    "html/template"
    "log"
    "net/http"
    "regexp"
    "strings"
)

var (
    // only allow letters, numbers, “_” and “-“, up to 30 chars
    validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

    // a tiny template that will HTML‐escape the username for us
    helloTmpl = template.Must(template.New("hello").
        Parse(`<p>Hello, {{.Username}}!</p>`))
)

func main() {
    // indexHandler now only responds on “/” (everything else → 404)
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)

    log.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    // only allow exactly “/”
    if r.URL.Path != "/" {
        http.NotFound(w, r)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte("<p>Hello, World!</p>"))
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only GET
    if r.Method != http.MethodGet {
        // per RFC, let client know we only allow GET here
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) extract the “<username>” piece out of /hello/<username>
    //    by trimming the known prefix.  If there are any extra “/” in it,
    //    it’s a 404.
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" || strings.Contains(username, "/") {
        http.NotFound(w, r)
        return
    }

    // 3) validate the username against our regexp
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 4) render via html/template (auto‐escaping)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    data := struct{ Username string }{Username: username}
    if err := helloTmpl.Execute(w, data); err != nil {
        log.Printf("template execute error: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
    }
}