package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract the username from the URL path
    pathParts := strings.SplitN(r.URL.Path, "/", 3)
    if len(pathParts) != 3 || pathParts[1] != "hello" || pathParts[2] == "" {
        http.Error(w, "Invalid path", http.StatusBadRequest)
        return
    }
    username := pathParts[2]

    // Sanitize the username to prevent XSS (example: only allow letters, numbers, and underscores)
    username = sanitizeUsername(username)

    // Greet the user
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}

// sanitizeUsername ensures only letters, numbers, and underscores are allowed.
func sanitizeUsername(input string) string {
    var buf strings.Builder
    for _, r := range input {
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '_' {
            buf.WriteRune(r)
        }
    }
    return buf.String()
}