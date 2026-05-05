package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "html/template"
    "net/http"
    "net/mail"
)

// session is just a placeholder; in a real app you’d
// want a proper session store keyed by a secure cookie
var session = make(map[string]string)

// settingsTemplate is an html/template to avoid XSS
var settingsTemplate = template.Must(template.New("settings").Parse(`
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>Settings</title></head>
<body>
  <h1>Account Settings</h1>
  <form action="/settings" method="post">
    <input type="hidden" name="csrf_token" value="{{.CSRFToken}}">
    <p>
      <label for="email">Email:</label>
      <input id="email" type="email" name="email" value="{{.Email}}" required>
    </p>
    <p>
      <label>
        <input type="checkbox" name="notify" {{if .Notify}}checked{{end}}>
        Enable notifications
      </label>
    </p>
    <button type="submit">Save Settings</button>
  </form>
</body>
</html>
`))

// settingsHandler handles both GET (render form) and POST (submit form)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET and POST
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // username is guaranteed non‐empty by ensureLoggedIn
    username := session["username"]

    switch r.Method {
    case http.MethodGet:
        // Generate a one‐time CSRF token, store in session
        csrfToken, err := generateCSRFToken()
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        session["csrf_token"] = csrfToken

        // Load existing settings for this user from your datastore.
        // Here we just mock them.
        existingEmail := "user@example.com"
        existingNotify := true

        // Render the form
        data := struct {
            CSRFToken string
            Email     string
            Notify    bool
        }{
            CSRFToken: csrfToken,
            Email:     existingEmail,
            Notify:    existingNotify,
        }

        if err := settingsTemplate.Execute(w, data); err != nil {
            http.Error(w, "Failed to render template", http.StatusInternalServerError)
            return
        }

    case http.MethodPost:
        // Parse and validate form inputs
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Bad request", http.StatusBadRequest)
            return
        }

        // 1) Validate CSRF token
        formToken := r.FormValue("csrf_token")
        if formToken == "" || formToken != session["csrf_token"] {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }
        // Invalidate the token immediately to prevent double‐POST
        delete(session, "csrf_token")

        // 2) Validate email address
        email := r.FormValue("email")
        if _, err := mail.ParseAddress(email); err != nil {
            http.Error(w, "Invalid email address", http.StatusBadRequest)
            return
        }

        // 3) Parse checkbox
        notify := false
        if r.FormValue("notify") == "on" {
            notify = true
        }

        // 4) Persist settings to your datastore, keyed by username.
        //    Here we'll just print them to the console.
        fmt.Printf("Updating settings for %q: email=%q, notify=%v\n",
            username, email, notify)

        // 5) Give the user feedback
        fmt.Fprint(w, `
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>Settings Saved</title></head>
<body>
  <p>Your settings have been updated.</p>
  <a href="/settings">Back to settings</a>
</body>
</html>
`)
    }
}

// generateCSRFToken returns a URL‐safe random string
func generateCSRFToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// --- wiring it all up ---
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // wrap settingsHandler in the login‐check decorator
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    fmt.Println("listening on :8080")
    http.ListenAndServe(":8080", nil)
}