package main

import (
    "fmt"
    "html/template" // Use context-aware output encoding to prevent XSS
    "net/http"
    "regexp"
)

// Security Note: Always use HTTPS in production to protect data in transit.
// For demonstration, this example uses HTTP. In production, configure TLS.

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security: ListenAndServeTLS should be used in production for HTTPS.
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by username extracted from the URL path.
Security:
- Strictly validates and sanitizes the username (alphanumeric, 3-20 chars).
- Uses html/template for context-aware output encoding to prevent XSS (CWE-79).
- Does not leak internal errors to the client.
- No sensitive data is processed or stored.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    username := r.URL.Path[len("/hello/"):]
    
    // Validate username: only allow 3-20 alphanumeric characters (no special chars)
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    if !validUsername.MatchString(username) {
        // Security: Do not reveal validation details to avoid user enumeration
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

    // Proper error handling: log error server-side, generic message to client
    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal server error.", http.StatusInternalServerError)
        // In production, log the error securely (not shown here)
    }
}
