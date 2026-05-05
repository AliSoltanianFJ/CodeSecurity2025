package main

import (
    "fmt"
    "html/template"
    "net/http"
    "strings"
)

var session = make(map[string]string)

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
        // Use a session cookie instead of global map for real-world apps
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
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic input validation
    if len(username) == 0 || len(username) > 32 || len(password) == 0 || len(password) > 64 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // In production, use hashed passwords and secure session management
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
                <h1>Secret Settings</h1>
                <p>Welcome, {{.Username | html}}</p>
                <!-- No sensitive data exposed -->
            </body>
        </html>
    `))

    username := session["username"]
    // Sanitize username for display
    if len(username) > 32 {
        http.Error(w, "Invalid session", http.StatusBadRequest)
        return
    }

    data := struct {
        Username string
    }{
        Username: username,
    }

    err := tmpl.Execute(w, data)
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
    }
}
