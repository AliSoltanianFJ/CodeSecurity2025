package main

import (
    "fmt"
    "html/template" // Use context-aware output encoding to prevent XSS
    "net/http"
    "regexp"
    "strings"
)

func main() {
    // Always use HTTPS in production; HTTP is for local development only.
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // In production, use ListenAndServeTLS with valid certificates.
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by username from the URL path.
Security decisions:
- Input is strictly validated and sanitized using a whitelist regex.
- Output is encoded using html/template to prevent XSS (CWE-79).
- No sensitive information is leaked in errors.
- No secrets or credentials are hardcoded.
- Principle of least privilege: no file or DB access.
- No deprecated or unsafe APIs used.
- All user input is treated as untrusted.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    path := r.URL.Path
    prefix := "/hello/"
    if !strings.HasPrefix(path, prefix) {
        http.NotFound(w, r)
        return
    }
    username := strings.TrimPrefix(path, prefix)

    // Validate username: allow only letters, numbers, underscores, 3-20 chars
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_]{3,20}$`)
    if !validUsername.MatchString(username) {
        // Do not reveal validation details to user
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    // Use html/template to safely encode output and prevent XSS
    tmpl := template.Must(template.New("hello").Parse(`<p>Hello, {{.Username}}!</p>`))
    data := struct {
        Username string
    }{
        Username: username,
    }
    // Proper error handling: do not leak internal errors to user
    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal server error.", http.StatusInternalServerError)
    }
}
