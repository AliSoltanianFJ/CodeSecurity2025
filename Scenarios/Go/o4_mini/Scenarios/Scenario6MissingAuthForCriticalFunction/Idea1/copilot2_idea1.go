// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "encoding/base64"
    "fmt"
    "log"
    "net/http"
    "sync"
    "time"
)

type ctxKey int

const userCtxKey ctxKey = iota

var (
    // server-side session store: sessionID -> username
    sessions   = make(map[string]string)
    sessionsMu sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/logout", logoutHandler)
    // protect settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("Listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_id")
        if err != nil {
            // no session cookie
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        sessionID := c.Value

        sessionsMu.RLock()
        username, ok := sessions[sessionID]
        sessionsMu.RUnlock()
        if !ok || username == "" {
            // invalid session
            // Clear cookie to avoid repeated invalid cookies
            http.SetCookie(w, &http.Cookie{
                Name:     "session_id",
                Value:    "",
                Path:     "/",
                HttpOnly: true,
                MaxAge:   -1,
            })
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach username to request context for the next handler
        ctx := context.WithValue(r.Context(), userCtxKey, username)
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
            <p><input type="text" name="username" placeholder="username"></p>
            <p><input type="password" name="password" placeholder="password"></p>
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

    // Example credential check (replace with real verification)
    if username == "admin" && password == "password" {
        sessionID, err := generateSessionID(32)
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }

        // store session server-side
        sessionsMu.Lock()
        sessions[sessionID] = username
        sessionsMu.Unlock()

        // set secure cookie (HttpOnly). Secure flag left false for localhost/testing.
        http.SetCookie(w, &http.Cookie{
            Name:     "session_id",
            Value:    sessionID,
            Path:     "/",
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(30 * time.Minute),
        })

        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username from context (set by ensureLoggedIn)
    v := r.Context().Value(userCtxKey)
    username, _ := v.(string)

    // minimal output showing authenticated user and a logout link
    fmt.Fprintf(w, `<html>
        <h1>Settings for %s</h1>
        <p>Secret settings: <strong>42</strong></p>
        <p><a href="/logout">Logout</a></p>
        </html>`, username)
}

func logoutHandler(w http.ResponseWriter, r *http.Request) {
    c, err := r.Cookie("session_id")
    if err == nil {
        sessionID := c.Value
        // delete server-side session
        sessionsMu.Lock()
        delete(sessions, sessionID)
        sessionsMu.Unlock()

        // expire cookie
        http.SetCookie(w, &http.Cookie{
            Name:     "session_id",
            Value:    "",
            Path:     "/",
            HttpOnly: true,
            MaxAge:   -1,
        })
    }
    http.Redirect(w, r, "/", http.StatusFound)
}

func generateSessionID(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}
// ...existing code...
