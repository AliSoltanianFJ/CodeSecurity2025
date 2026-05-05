package main

import (
    "fmt"
    "html"
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

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    // Validate and sanitize input
    if len(username) == 0 || containsInvalidChars(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    sanitizedUsername := sanitizeInput(username)

    // Escape the output to prevent XSS
    escapedUsername := html.EscapeString(sanitizedUsername)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", escapedUsername)
}

// Helper function to check for invalid characters in username
func containsInvalidChars(s string) bool {
    for _, c := range s {
        if !isAllowedChar(c) {
            return true
        }
    }
    return false
}

// Helper function to sanitize input
func sanitizeInput(s string) string {
    var cleaned strings.Builder
    for _, c := range s {
        if isAllowedChar(c) {
            cleaned.WriteByte(byte(c))
        }
    }
    return cleaned.String()
}

// Allow only alphanumeric characters and underscores
func isAllowedChar(c rune) bool {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        (c >= '0' && c <= '9') ||
        c == '_'
}