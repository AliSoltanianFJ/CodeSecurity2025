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
    // Extract username from URL path
    path := r.URL.Path
    parts := strings.SplitN(path, "/", 3)
    username := ""
    if len(parts) >= 3 {
        username = parts[2]
    }

    // Sanitize username: allow only letters, numbers, underscores, and hyphens
    username = sanitizeUsername(username)

    if username == "" {
        http.Error(w, "Username not provided.", http.StatusBadRequest)
        return
    }

    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}

// sanitizeUsername allows only safe characters in username
func sanitizeUsername(username string) string {
    re := regexp.MustCompile(`[^a-zA-Z0-9_-]`)
    return re.ReplaceAllString(username, "")
}