package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
    "time"
)

var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{1,50}$`)

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/", indexHandler)
    mux.HandleFunc("/hello/", helloHandler)

    srv := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
    }

    fmt.Println("Listening on http://localhost:8080")
    if err := srv.ListenAndServe(); err != nil {
        fmt.Println("Server error:", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>\n")
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Allow only GET
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Extract the username portion from the URL.
    //
    //    The pattern "/hello/" (with the trailing slash) was used in
    //    HandleFunc, so any URL beginning "/hello/" will get here.
    //    We strip off exactly that prefix.  If the user visits "/hello"
    //    (no trailing slash) this handler will *not* be invoked.
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if len(username) == 0 {
        http.Error(w, "Username not specified", http.StatusBadRequest)
        return
    }

    // 3) Validate that it’s not too long and only contains safe characters.
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 4) HTML-escape the user input before embedding in a response.
    safe := html.EscapeString(username)

    // 5) Write out a simple HTML response
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<!DOCTYPE html>\n"+
        "<html><head><meta charset=\"utf-8\"><title>Hello</title></head>\n"+
        "<body>\n"+
        "  <p>Hello, %s!</p>\n"+
        "</body>\n"+
        "</html>\n", safe)
}