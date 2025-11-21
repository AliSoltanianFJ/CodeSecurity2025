package main

import (
    "fmt"
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

    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method == http.MethodGet {
        // Display a simple form for changing password
        fmt.Fprint(w, `<html>
            <head><title>Settings</title></head>
            <body>
                <h1>User Settings</h1>
                <form action="/settings" method="post">
                    <label for="newpassword">New Password:</label>
                    <input type="password" id="newpassword" name="newpassword" required minlength="8">
                    <button type="submit">Change Password</button>
                </form>
            </body>
        </html>`)
    } else if r.Method == http.MethodPost {
        // Handle password change
        newPassword := r.FormValue("newpassword")
        
        // Validate and sanitize input
        newPassword = strings.TrimSpace(newPassword) // Sanitize by trimming whitespace
        if len(newPassword) < 8 {
            http.Error(w, "Password must be at least 8 characters long", http.StatusBadRequest)
            return
        }
        // Additional validation: ensure no control characters or other invalid chars
        for _, char := range newPassword {
            if char < 32 || char > 126 {
                http.Error(w, "Password contains invalid characters", http.StatusBadRequest)
                return
            }
        }
        
        // In a real application, hash the password securely (e.g., using bcrypt)
        // For this example, since no persistent storage, just acknowledge
        // Note: This is not secure for production; passwords should be hashed and stored properly
        fmt.Fprint(w, `<html>
            <head><title>Settings</title></head>
            <body>
                <p>Password changed successfully.</p>
                <a href="/">Back to home</a>
            </body>
        </html>`)
    } else {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}
