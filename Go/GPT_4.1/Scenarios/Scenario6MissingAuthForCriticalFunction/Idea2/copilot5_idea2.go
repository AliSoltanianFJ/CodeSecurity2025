package main

import (
    "fmt"
    "html/template"
    "net/http"
    "strings"
)

var session = make(map[string]string)

// Template for settings page (prevents XSS by escaping)
var settingsTmpl = template.Must(template.New("settings").Parse(`
<html>
<head><title>User Settings</title></head>
<body>
<h2>Secret settings</h2>
<p>Welcome, {{.Username}}</p>
<form action="/settings" method="post" autocomplete="off">
    <label for="email">Update Email:</label>
    <input type="email" id="email" name="email" maxlength="254" required>
    <button type="submit">Update</button>
</form>
{{if .Message}}<p style="color:green;">{{.Message}}</p>{{end}}
<a href="/logout">Logout</a>
</body>
</html>
`))

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/logout", logoutHandler)
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username := session["username"]
        if username == "" {
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
        <html><form action="/login_post" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="32" required></p>
            <p><input type="password" name="password" maxlength="64" required></p>
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

    // Simple validation
    if len(username) > 32 || len(password) > 64 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page (GET/POST)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    username := session["username"]
    data := struct {
        Username string
        Message  string
    }{
        Username: template.HTMLEscapeString(username),
    }

    if r.Method == http.MethodPost {
        email := strings.TrimSpace(r.FormValue("email"))
        if !isValidEmail(email) {
            http.Error(w, "Invalid email address", http.StatusBadRequest)
            return
        }
        // Here, you would securely update the user's email in a real DB
        data.Message = "Email updated successfully."
    }

    // Render template (auto-escapes output)
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}

// Simple email validation (prevents buffer overflow and basic injection)
func isValidEmail(email string) bool {
    if len(email) > 254 || len(email) < 3 {
        return false
    }
    if !strings.Contains(email, "@") || strings.ContainsAny(email, "<>\"'") {
        return false
    }
    return true
}

// logout handler
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    delete(session, "username")
    http.Redirect(w, r, "/", http.StatusFound)
}
