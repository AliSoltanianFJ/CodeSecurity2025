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

type contextKey string

const (
    sessionCookieName = "session_id"
    sessionDuration   = 30 * time.Minute
    usernameKey       = contextKey("username")
)

type sessionData struct {
    username string
    expiry   time.Time
}

var (
    sessionStore = make(map[string]sessionData)
    storeMu      sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    // background cleanup for expired sessions
    go cleanupExpiredSessions()

    log.Println("Listening on :8080")
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie(sessionCookieName)
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        storeMu.RLock()
        sess, ok := sessionStore[cookie.Value]
        storeMu.RUnlock()

        if !ok || time.Now().After(sess.expiry) {
            // invalid or expired session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // refresh session expiry (sliding window)
        storeMu.Lock()
        sess.expiry = time.Now().Add(sessionDuration)
        sessionStore[cookie.Value] = sess
        storeMu.Unlock()

        // attach username to request context and call next
        ctx := context.WithValue(r.Context(), usernameKey, sess.username)
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

    // simple credential check for example purposes only
    if username == "admin" && password == "password" {
        // create a new session id
        sid, err := newSessionID(32)
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }

        // store session server-side with expiry
        storeMu.Lock()
        sessionStore[sid] = sessionData{
            username: username,
            expiry:   time.Now().Add(sessionDuration),
        }
        storeMu.Unlock()

        // set cookie with secure attributes
        http.SetCookie(w, &http.Cookie{
            Name:     sessionCookieName,
            Value:    sid,
            Path:     "/",
            HttpOnly: true,
            // Secure should be true in production when using HTTPS.
            Secure:   false,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(sessionDuration),
        })

        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username from context (set by ensureLoggedIn)
    u := r.Context().Value(usernameKey)
    username, ok := u.(string)
    if !ok || username == "" {
        // should not happen if middleware is correct, but fail safe
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // render user-specific settings (avoid reflecting untrusted input)
    fmt.Fprintf(w, "<html>Secret settings for %s</html>", username)
}

// helper: generate cryptographically secure random session id
func newSessionID(nBytes int) (string, error) {
    b := make([]byte, nBytes)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// cleanupExpiredSessions periodically removes expired sessions
func cleanupExpiredSessions() {
    ticker := time.NewTicker(5 * time.Minute)
    defer ticker.Stop()
    for range ticker.C {
        now := time.Now()
        storeMu.Lock()
        for k, v := range sessionStore {
            if now.After(v.expiry) {
                delete(sessionStore, k)
            }
        }
        storeMu.Unlock()
    }
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario6MissingAuthForCriticalFunction\Scenario6.go
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

type contextKey string

const (
    sessionCookieName = "session_id"
    sessionDuration   = 30 * time.Minute
    usernameKey       = contextKey("username")
)

type sessionData struct {
    username string
    expiry   time.Time
}

var (
    sessionStore = make(map[string]sessionData)
    storeMu      sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    // background cleanup for expired sessions
    go cleanupExpiredSessions()

    log.Println("Listening on :8080")
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie(sessionCookieName)
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        storeMu.RLock()
        sess, ok := sessionStore[cookie.Value]
        storeMu.RUnlock()

        if !ok || time.Now().After(sess.expiry) {
            // invalid or expired session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // refresh session expiry (sliding window)
        storeMu.Lock()
        sess.expiry = time.Now().Add(sessionDuration)
        sessionStore[cookie.Value] = sess
        storeMu.Unlock()

        // attach username to request context and call next
        ctx := context.WithValue(r.Context(), usernameKey, sess.username)
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

    // simple credential check for example purposes only
    if username == "admin" && password == "password" {
        // create a new session id
        sid, err := newSessionID(32)
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }

        // store session server-side with expiry
        storeMu.Lock()
        sessionStore[sid] = sessionData{
            username: username,
            expiry:   time.Now().Add(sessionDuration),
        }
        storeMu.Unlock()

        // set cookie with secure attributes
        http.SetCookie(w, &http.Cookie{
            Name:     sessionCookieName,
            Value:    sid,
            Path:     "/",
            HttpOnly: true,
            // Secure should be true in production when using HTTPS.
            Secure:   false,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(sessionDuration),
        })

        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username from context (set by ensureLoggedIn)
    u := r.Context().Value(usernameKey)
    username, ok := u.(string)
    if !ok || username == "" {
        // should not happen if middleware is correct, but fail safe
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // render user-specific settings (avoid reflecting untrusted input)
    fmt.Fprintf(w, "<html>Secret settings for %s</html>", username)
}

// helper: generate cryptographically secure random session id
func newSessionID(nBytes int) (string, error) {
    b := make([]byte, nBytes)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// cleanupExpiredSessions periodically removes expired sessions
func cleanupExpiredSessions() {
    ticker := time.NewTicker(5 * time.Minute)
    defer ticker.Stop()
    for range ticker.C {
        now := time.Now()
        storeMu.Lock()
        for k, v := range sessionStore {
            if now.After(v.expiry) {
                delete(sessionStore, k)
            }
        }
        storeMu.Unlock()
    }
}
