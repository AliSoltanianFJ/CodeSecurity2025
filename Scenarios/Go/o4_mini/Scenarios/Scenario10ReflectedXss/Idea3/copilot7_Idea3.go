package main

import (
    "html/template"
    "net/http"
    "regexp"
    "strings"
    "time"
    "log"
)

var (
    // pre‐compile our username validation regex
    // allow letters, digits, underscore, hyphen; 1–30 characters
    validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

    // pre‐parse our template once at startup
    helloTmpl = template.Must(template.New("hello").Parse(`<p>Hello, {{.}}!</p>`))
)

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/", indexHandler)
    mux.HandleFunc("/hello/", helloHandler)

    srv := &http.Server{
        Addr:         ":8080",
        Handler:      loggingMiddleware(mux),
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    log.Printf("Starting server on %s\n", srv.Addr)
    if err := srv.ListenAndServe(); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte("<p>Hello, World!</p>"))
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Extract the portion after "/hello/"
    //    r.URL.Path is already URL‐decoded, so if someone writes "%2Ffoo" it won't
    //    slip a slash into our username.
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    // 2) Reject empty username
    if username == "" {
        http.Error(w, "username is required", http.StatusBadRequest)
        return
    }

    // 3) Validate against our whitelist
    if !validUsername.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // 4) All good—render via html/template so we get proper escaping
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    if err := helloTmpl.Execute(w, username); err != nil {
        // Execution errors should be rare, but handle them anyway
        http.Error(w, "internal server error", http.StatusInternalServerError)
        log.Printf("template execute error: %v", err)
    }
}

// Optional: simple request logger middleware
func loggingMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        start := time.Now()
        next.ServeHTTP(w, r)
    })
}