package main

import (
    "crypto/rand"
    "crypto/subtle"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "regexp"
    "sync"
    "time"
)

// ...existing code...
// replaced insecure global session map with a safe session store
type sessionData struct {
    Username string
    Expiry   time.Time
}

var (
    // in-memory session store protected by a mutex
    sessionStore = struct {
        sync.RWMutex
        m map[string]sessionData
    }{m: make(map[string]sessionData)}

    // simple username/password policy for this demo
    usernamePattern = regexp.MustCompile(`^[A-Za-z0-9_]{1,32}$`)
    maxPasswordLen   = 128
    sessionTTL       = 24 * time.Hour
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect the settings endpoint with the authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_token")
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        token := cookie.Value

        // basic validation of token format: hex of reasonable length
        if len(token) < 32 {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        sessionStore.RLock()
        sd, ok := sessionStore.m[token]
        sessionStore.RUnlock()
        if !ok || time.Now().After(sd.Expiry) || sd.Username == "" {
            // invalid or expired session
            // remove expired token if present
            sessionStore.Lock()
            delete(sessionStore.m, token)
            sessionStore.Unlock()

            // clear cookie
            http.SetCookie(w, &http.Cookie{
                Name:     "session_token",
                Value:    "",
                Path:     "/",
                HttpOnly: true,
                MaxAge:   -1,
            })
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach username in request context by using header (minimal, safe here)
        // avoid exposing sensitive info; we only set a header for downstream use
        r.Header.Set("X-Auth-Username", sd.Username)

        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href="./login">Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // simple CSRF protection would be added in a real app (e.g., per-form token)
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="32" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
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

    // ParseForm to ensure form values are available and limited in size by default
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Validate input: allow only safe username chars and reasonable lengths
    if !usernamePattern.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) == 0 || len(password) > maxPasswordLen {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Authenticate: use constant-time comparison for password checks
    // NOTE: hard-coded credential only for this demo. In real apps, use a secure user store and bcrypt/scrypt/argon2.
    expectedUser := "admin"
    expectedPass := "password"

    userMatch := subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1
    passMatch := subtle.ConstantTimeCompare([]byte(password), []byte(expectedPass)) == 1

    if userMatch && passMatch {
        token, err := generateSessionToken()
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }

        // store session server-side with expiry
        sessionStore.Lock()
        sessionStore.m[token] = sessionData{
            Username: username,
            Expiry:   time.Now().Add(sessionTTL),
        }
        sessionStore.Unlock()

        // set secure cookie flags; Secure true when using TLS
        secureFlag := false
        if r.TLS != nil {
            secureFlag = true
        }
        http.SetCookie(w, &http.Cookie{
            Name:     "session_token",
            Value:    token,
            Path:     "/",
            HttpOnly: true,
            Secure:   secureFlag,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(sessionTTL),
        })

        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Avoid revealing which part failed
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve authenticated username from header set in middleware
    username := r.Header.Get("X-Auth-Username")
    if username == "" {
        // fallback: should not happen due to middleware, but be defensive
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Use html/template to automatically escape output and prevent XSS
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Secret settings</h1>
            <p>Welcome, {{ .Username }}.</p>
            <!-- Do not display any sensitive details here -->
            <p>Account settings are available to authenticated users only.</p>
        </body>
        </html>
    `))

    data := struct {
        Username string
    }{
        Username: username,
    }

    // Execute template (escaped) and handle any error
    if err := tmpl.Execute(w, data); err != nil {
        // don't leak internal details to the user
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}

// generateSessionToken returns a cryptographically secure random token hex-encoded
func generateSessionToken() (string, error) {
    b := make([]byte, 32) // 256-bit
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
// ...existing code...
