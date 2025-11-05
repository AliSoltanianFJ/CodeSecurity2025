package main

import (
    "fmt"
    "html/template"
    "net/http"
    "regexp"
)

var session = make(map[string]string)

// Simple username validation (alphanumeric, 3-20 chars)
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        if session["username"] == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate input
    if !validUsername.MatchString(username) || len(password) < 6 || len(password) > 64 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Use html/template to prevent XSS
    tmpl := template.Must(template.New("settings").Parse(`
        <html>
            <head><title>User Settings</title></head>
            <body>
                <h2>Welcome, {{.Username}}</h2>
                <p>This is your secret settings page.</p>
                <a href="/">Home</a>
            </body>
        </html>
    `))

    username := session["username"]
    // Sanitize username (already validated, but escape anyway)
    data := struct {
        Username string
    }{
        Username: template.HTMLEscapeString(username),
    }

    // Prevent template injection and XSS
    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
    }
}
