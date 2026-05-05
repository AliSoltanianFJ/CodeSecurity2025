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

type ctxKey string

const sessionCookieName = "session_id"
const ctxUserKey ctxKey = "username"

var (
    // in-memory session store (for demo). Protect with a mutex for concurrency.
    sessionStore = make(map[string]string)
    sessMu       sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect the settings endpoint with authentication
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    log.Println("Listening on :8080")
    //-copilot next line-
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie(sessionCookieName)
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        sessMu.RLock()
        username, ok := sessionStore[cookie.Value]
        sessMu.RUnlock()
        if !ok || username == "" {
            // Invalid session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach username to context for the next handler
        ctx := context.WithValue(r.Context(), ctxUserKey, username)
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

    // simple credential check for demo purposes
    if username == "admin" && password == "password" {
        // create a secure random session id
        sid, err := newSessionID(32)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }

        // store session
        sessMu.Lock()
        sessionStore[sid] = username
        sessMu.Unlock()

        // set cookie (HttpOnly, SameSite)
        http.SetCookie(w, &http.Cookie{
            Name:     sessionCookieName,
            Value:    sid,
            Path:     "/",
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(24 * time.Hour),
            // Secure: true, // enable in production (requires HTTPS)
        })

        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username from context (set by ensureLoggedIn)
    username, _ := r.Context().Value(ctxUserKey).(string)
    if username == "" {
        // fallback: deny access
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }

    // render settings for authenticated user
    fmt.Fprintf(w, `<html><h1>Settings for %s</h1><p>Secret settings</p></html>`, username)
}

// newSessionID generates a URL-safe base64 session id with n bytes of entropy.
func newSessionID(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}
// ...existing code...
