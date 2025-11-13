package main

import (
    "fmt"
    "net/http"
    "time"
)

var sessions = make(map[string]string) // Key: session ID, Value: username
const sessionCookieName = "session_id"

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/logout", logoutHandler)
    http.ListenAndServe(":8080", nil)
}

// Decorator to ensure user is logged in
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        sessionID, err := r.Cookie(sessionCookieName)
        if err != nil || len(sessions[sessionID.Value]) == 0 {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// Home page handler
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='/login'>Login</a> | <a href='/logout'>Logout</a></html>`)
}

// Login page handler (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p>Username: <input type="text" name="username"></p>
            <p>Password: <input type="password" name="password"></p>
            <p><button type="submit">Login</button></p>
        </form></html>
    `)
}

// Login POST handler
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "admin" && password == "password" {
        sessionID := generateSessionID()
        sessions[sessionID] = username
        cookie := &http.Cookie{
            Name:    sessionCookieName,
            Value:   sessionID,
            Expires: time.Now().Add(24 * time.Hour),
        }
        http.SetCookie(w, cookie)
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Invalid username or password. <a href='/login'>Try again</a>")
    }
}

// Generate a random session ID (simplified)
func generateSessionID() string {
    return time.Now().String()
}

// Settings handler
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Secret settings page. <a href='/logout'>Logout</a></html>`)
}

// Logout handler
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    cookie := &http.Cookie{
        Name:    sessionCookieName,
        Value:   "",
        Expires: time.Now().Add(-1 * time.Hour),
    }
    http.SetCookie(w, cookie)
    http.Redirect(w, r, "/", http.StatusFound)
}