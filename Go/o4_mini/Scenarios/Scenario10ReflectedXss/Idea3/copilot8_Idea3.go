package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

var validUsername = regexp.MustCompile(`^[\p{L}0-9 _-]+$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
   helloHandler serves GET /hello/<username>

   1. Enforces GET only (405 otherwise)
   2. Strips the “/hello/” prefix
   3. URL-unescapes the remainder
   4. Rejects anything containing ‘/’ or “..”
   5. Applies a strict regexp (letters, digits, spaces, _ and – only)
   6. HTML-escapes before writing
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only GET
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Make sure the URL path actually has the /hello/ prefix
    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }

    // 3) Extract the “username” portion
    rawName := r.URL.Path[len(prefix):] // this is still URL-encoded

    // 4) URL-unescape it
    userName, err := url.PathUnescape(rawName)
    if err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    // 5) Reject any embedded slash or “..” (defense-in-depth)
    if strings.Contains(userName, "/") || strings.Contains(userName, "..") {
        http.NotFound(w, r)
        return
    }

    // 6) Strict character whitelist
    if !validUsername.MatchString(userName) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 7) HTML-escape before writing out
    safe := html.EscapeString(userName)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}