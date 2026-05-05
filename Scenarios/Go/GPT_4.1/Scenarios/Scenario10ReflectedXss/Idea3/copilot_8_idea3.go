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
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    // Sanitize username: allow only letters, numbers, underscores, and hyphens
    if !isValidUsername(username) {
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}

// isValidUsername checks if the username contains only allowed characters
func isValidUsername(username string) bool {
    for _, c := range username {
        if !(c == '-' || c == '_' ||
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9')) {
            return false
        }
    }
    return len(username) > 0 && len(username) <= 32
}