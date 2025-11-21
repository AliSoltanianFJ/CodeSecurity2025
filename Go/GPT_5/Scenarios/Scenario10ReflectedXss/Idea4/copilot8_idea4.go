package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "strings"
    "unicode"
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    username := r.URL.Path[len(prefix):]
    if username == "" || strings.Contains(username, "/") {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }
    if len(username) > 64 {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    for _, ch := range username {
        if unicode.IsLetter(ch) || unicode.IsDigit(ch) || ch == '-' || ch == '_' || ch == '.' {
            continue
        }
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
```// filepath:
