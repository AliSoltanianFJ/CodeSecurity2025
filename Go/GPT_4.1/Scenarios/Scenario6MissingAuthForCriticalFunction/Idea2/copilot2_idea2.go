package main

import (
    "fmt"
    "html/template"
    "net/http"
    "strings"
)

var session = make(map[string]string)

// Secure template for settings page
var settingsTmpl = template.Must(template.New("settings").Parse(`
<html>
<head><title>User Settings</title></head>
<body>
<h2>Welcome, {{.Username | html}}</h2>
<p>Secret settings are only visible to authenticated users.</p>
<form action="/logout" method="post">
    <button type="submit">Logout</button>
</form>
</body>
</html>
`))

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/logout", ensureLoggedIn(logoutHandler))
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
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="32" pattern="[a-zA-Z0-9_]+" required></p>
            <p><input type="password" name="password" maxlength="32" required></p>
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

    // Input validation
    if len(username) == 0 || len(username) > 32 || len(password) == 0 || len(password) > 32 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }
    if !isAlphanumeric(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    username := session["username"]
    // Never expose sensitive info, only show sanitized username
    err := settingsTmpl.Execute(w, struct{ Username string }{Username: username})
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
    }
}

// logout handler
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    delete(session, "username")
    http.Redirect(w, r, "/", http.StatusFound)
}

// Helper: check if string is alphanumeric or underscore
func isAlphanumeric(s string) bool {
    for _, c := range s {
        if !(c >= 'a' && c <= 'z') &&
            !(c >= 'A' && c <= 'Z') &&
            !(c >= '0' && c <= '9') &&
            c != '_' {
            return false
        }
    }
    return true
}
