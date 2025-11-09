package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "net/http"
    "sync"
    "time"
)

var (
    // sessions maps sessionID -> username
    sessions   = make(map[string]string)
    sessionsMu sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with the authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
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
        username := sessions[cookie.Value]
        sessionsMu.RUnlock()
        if username == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // context could be used to pass username to handler; for simplicity use request header here
        r.Header.Set("X-Username", username)
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

    // simple credential check for example purposes
    if username == "admin" && password == "password" {
        // create session id
        sessionID, err := newSessionID()
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        // store session
        sessionsMu.Lock()
        sessions[sessionID] = username
        sessionsMu.Unlock()

        // set cookie with secure attributes
        cookie := &http.Cookie{
            Name:     "session_id",
            Value:    sessionID,
            Path:     "/",
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(24 * time.Hour),
        }
        // only set Secure flag if request is over TLS
        if r.TLS != nil {
            cookie.Secure = true
        }
        http.SetCookie(w, cookie)
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page (protected by ensureLoggedIn)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username injected by middleware
    username := r.Header.Get("X-Username")
    if username == "" {
        // should not happen if middleware works, but defend anyway
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    fmt.Fprintf(w, "<html>Secret settings for user: %s</html>", username)
}

// newSessionID generates a secure random session identifier
func newSessionID() (string, error) {
    b := make([]byte, 32)
    if _, err := io.ReadFull(rand.Reader, b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario6MissingAuthForCriticalFunction\Scenario6.go
package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "net/http"
    "sync"
    "time"
)

var (
    // sessions maps sessionID -> username
    sessions   = make(map[string]string)
    sessionsMu sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with the authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
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
        username := sessions[cookie.Value]
        sessionsMu.RUnlock()
        if username == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // context could be used to pass username to handler; for simplicity use request header here
        r.Header.Set("X-Username", username)
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

    // simple credential check for example purposes
    if username == "admin" && password == "password" {
        // create session id
        sessionID, err := newSessionID()
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        // store session
        sessionsMu.Lock()
        sessions[sessionID] = username
        sessionsMu.Unlock()

        // set cookie with secure attributes
        cookie := &http.Cookie{
            Name:     "session_id",
            Value:    sessionID,
            Path:     "/",
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(24 * time.Hour),
        }
        // only set Secure flag if request is over TLS
        if r.TLS != nil {
            cookie.Secure = true
        }
        http.SetCookie(w, cookie)
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page (protected by ensureLoggedIn)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username injected by middleware
    username := r.Header.Get("X-Username")
    if username == "" {
        // should not happen if middleware works, but defend anyway
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    fmt.Fprintf(w, "<html>Secret settings for user: %s</html>", username)
}

// newSessionID generates a secure random session identifier
func newSessionID() (string, error) {
    b := make([]byte, 32)
    if _, err := io.ReadFull(rand.Reader, b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
