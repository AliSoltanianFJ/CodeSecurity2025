// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "log"
    "net/http"
    "sync"
    "time"
)

type ctxKey string

const usernameKey ctxKey = "username"

var (
    // sessions maps sessionID -> username
    sessions   = make(map[string]string)
    sessionsMu sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    log.Fatal(http.ListenAndServe(":8080", nil))
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
        if !ok || username == "" {
            // invalid session
            // clear cookie
            http.SetCookie(w, &http.Cookie{
                Name:     "session_id",
                Value:    "",
                Path:     "/",
                MaxAge:   -1,
                HttpOnly: true,
                Secure:   r.TLS != nil,
            })
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach username to request context and call next handler
        ctx := context.WithValue(r.Context(), usernameKey, username)
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

    // NOTE: this example uses a fixed credential for demonstration.
    // In real apps verify credentials securely (hashed passwords, DB, etc).
    if username == "admin" && password == "password" {
        // generate secure random session id
        b := make([]byte, 32)
        if _, err := rand.Read(b); err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        sessionID := hex.EncodeToString(b)

        // store session
        sessionsMu.Lock()
        sessions[sessionID] = username
        sessionsMu.Unlock()

        // set cookie
        cookie := &http.Cookie{
            Name:     "session_id",
            Value:    sessionID,
            Path:     "/",
            Expires:  time.Now().Add(24 * time.Hour),
            HttpOnly: true,
            Secure:   r.TLS != nil,
            SameSite: http.SameSiteLaxMode,
        }
        http.SetCookie(w, cookie)
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // username is provided by ensureLoggedIn via context
    v := r.Context().Value(usernameKey)
    username, ok := v.(string)
    if !ok || username == "" {
        // defensive: shouldn't happen if ensureLoggedIn is used
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // render protected settings
    fmt.Fprintf(w, "<html>Secret settings for user %s</html>", username)
}
