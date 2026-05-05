package main

import (
    "fmt"
    "net/http"
    "sync"
)

// Use a mutex to protect the global session map for thread safety
var sessionMutex sync.RWMutex
var session = make(map[string]string)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply ensureLoggedIn decorator to settingsHandler to enforce authentication
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        sessionMutex.RLock()
        username := session["username"]
        sessionMutex.RUnlock()
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

    // Hardcoded credentials for demonstration; in production, use hashed passwords from a database
    // Security note: This is insecure; replace with proper authentication using bcrypt or similar
    if username == "admin" && password == "password" {
        sessionMutex.Lock()
        session["username"] = username
        sessionMutex.Unlock()
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page - now secured with authentication
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Security note: This page is protected by ensureLoggedIn, preventing unauthorized access
    // In a real application, use HTTPS to protect data in transit
    // Add CSRF protection, input validation, and use html/template for safe rendering
    fmt.Fprint(w, "<html>Secret settings <a href='/logout'>Logout</a></html>")
}
