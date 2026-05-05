package main

import (
	"fmt"
	"net/http"
	"html"
	"strings"
	"errors"
	"regexp"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by name in a secure manner.
The username is extracted from the URL path and validated according to strict security policies.
All output is properly escaped to prevent XSS attacks. Sensitive error handling ensures no internal details are exposed.

Method: GET, HEAD
Path: /hello/{username}
Params: None

Response:
  200 OK - A friendly greeting with proper HTML escaping.
         - Includes security headers to prevent caching and content type sniffing.
  400 Bad Request - Invalid username provided (e.g., empty, too long/short, or contains invalid characters).
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Set secure headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "no-store")

    // Only allow expected methods
    if !(r.Method == http.GET || r.Method == http.HEAD) {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse username from URL path
    path := r.URL.Path[len("/hello/"):]
    user := User{Username: strings.TrimSpace(path)}

    // Validate username (see below for implementation)
    if err := user.isValidUsername(); err != nil {
        http.Error(w, "Invalid username provided", http.StatusBadRequest)
        return
    }

    // Escape username to prevent XSS
    safeUsername := html.EscapeString(user.Username)

    // Output greeting with HTML escaping
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// User represents a system user with validated credentials.
type User struct {
    Username string
}

/*
isValidUsername validates the username according to security policies:
- Must be 3-32 characters long (RFC-compliant)
- Only allow alphanumeric and underscore characters
- Disallow reserved usernames starting with 'admin'
*/
func (u *User) isValidUsername() error {
    if len(u.Username) < 3 || len(u.Username) > 32 {
        return errors.New("username length invalid")
    }
    matched, _ := regexp.MatchString(`^[a-zA-Z0-9_]+$`, u.Username)
    if !matched {
        return errors.New("invalid username characters")
    }
    if strings.HasPrefix(strings.ToLower(u.Username), "admin") {
        return errors.New("reserved username pattern")
    }
    return nil
}
