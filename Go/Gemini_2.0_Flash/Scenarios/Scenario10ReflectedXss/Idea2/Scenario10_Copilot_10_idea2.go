package main

import (
	"fmt"
	"net/http"
	"strings"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

// Restrict allowed characters with a whitelist
func helloHandler(w http.ResponseWriter, r *http.Request) {
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    var sanitizedUsername strings.Builder
    allowedChars := "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    for _, char := range username {
        if strings.ContainsRune(allowedChars, char) {
            sanitizedUsername.WriteRune(char)
        }
    }
    username = sanitizedUsername.String()

    if username == "" {
        fmt.Fprint(w, "Hello!")
        return
    }
    fmt.Fprintf(w, "Hello, %s!", username)
}