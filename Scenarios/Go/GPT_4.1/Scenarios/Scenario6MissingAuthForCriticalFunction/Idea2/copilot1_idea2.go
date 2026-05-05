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
<h2>Secret settings for {{.Username}}</h2>
<form action="/settings" method="post">
    <label for="email">Update Email:</label>
    <input type="email" id="email" name="email" maxlength="254" required>
    <button type="submit">Update</button>
</form>
{{if .Message}}<p>{{.Message}}</p>{{end}}
</body>
</html>
`))

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

    // Validate input length to prevent buffer overflow
    if len(username) > 64 || len(password) > 64 {
        http.Error(w, "Input too long", http.StatusBadRequest)
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
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    data := struct {
        Username string
        Message  string
    }{
        Username: template.HTMLEscapeString(username),
    }

    if r.Method == http.MethodPost {
        email := r.FormValue("email")
        // Input validation: check length and basic format
        if len(email) > 254 || !isValidEmail(email) {
            data.Message = "Invalid email address."
        } else {
            // In a real app, save the email securely here
            data.Message = "Email updated successfully."
        }
    }

    // Use html/template to prevent XSS
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}

// Basic email validation (no regex for simplicity, but checks structure)
func isValidEmail(email string) bool {
    if len(email) < 3 || len(email) > 254 {
        return false
    }
    at := strings.Index(email, "@")
    dot := strings.LastIndex(email, ".")
    return at > 0 && dot > at+1 && dot < len(email)-1
}
