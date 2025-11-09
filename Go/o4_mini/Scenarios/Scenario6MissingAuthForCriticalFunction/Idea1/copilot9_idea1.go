package main

import (
    "context"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "sync"
    "time"
)

// ...existing code...

var (
    sessions   = make(map[string]string) // sessionID -> username
    sessionsMu sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with the ensureLoggedIn wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_id")
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        sessionsMu.RLock()
        username, ok := sessions[cookie.Value]
        sessionsMu.RUnlock()
        if !ok {
            // unknown/expired session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // place username in context for handlers to use
        ctx := context.WithValue(r.Context(), "username", username)
        next(w, r.WithContext(ctx))
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

    // simple credential check for demo; replace with real auth in production
    if username == "admin" && password == "password" {
        // create session
        sessionID, err := newSessionID()
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        sessionsMu.Lock()
        sessions[sessionID] = username
        sessionsMu.Unlock()

        setSessionCookie(w, sessionID)
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

func newSessionID() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

func setSessionCookie(w http.ResponseWriter, sessionID string) {
    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true in production when using HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(1 * time.Hour),
    }
    http.SetCookie(w, cookie)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username set by ensureLoggedIn
    username, _ := r.Context().Value("username").(string)

    tmpl := template.Must(template.New("settings").Parse(`
        <html>
            <head><title>Settings</title></head>
            <body>
                <h1>Settings for {{.Username}}</h1>
                <p>Secret settings</p>
                <form action="/logout" method="post">
                    <button type="submit">Logout</button>
                </form>
            </body>
        </html>
    `))

    data := struct {
        Username string
    }{
        Username: username,
    }

    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Template error", http.StatusInternalServerError)
    }
}
