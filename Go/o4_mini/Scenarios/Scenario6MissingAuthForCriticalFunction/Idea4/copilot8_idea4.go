package main

import (
    "crypto/rand"
    "crypto/subtle"
    "encoding/base64"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// Simple in-memory session store (for demo only). In production use a hardened store.
type sessionData struct {
    Username  string
    Expires   time.Time
    CSRFToken string
}

var (
    // sessions guarded by mutex for concurrency safety
    sessions   = make(map[string]sessionData)
    sessionsMu sync.RWMutex

    // admin password hash (demo). In production store hashed passwords in a protected DB/secrets manager.
    adminHash []byte
)

const (
    sessionCookieName = "session_id"
    sessionTTL        = 30 * time.Minute
    maxInputLen       = 128
)

func init() {
    // Create a bcrypt hash for the demo password "password".
    // In production use a strong password policy and store hashes outside source code.
    var err error
    adminHash, err = bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to generate admin hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    // logout to demonstrate session termination
    http.HandleFunc("/logout", ensureLoggedIn(logoutHandler))

    // Note: In production, ServeTLS with valid certs or run behind TLS-terminating reverse proxy.
    log.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// ensureLoggedIn decorator: validates session cookie and expiry
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Read cookie
        c, err := r.Cookie(sessionCookieName)
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // Lookup session
        sessionsMu.RLock()
        sess, ok := sessions[c.Value]
        sessionsMu.RUnlock()
        if !ok || time.Now().After(sess.Expires) {
            // delete expired session if present
            if ok {
                sessionsMu.Lock()
                delete(sessions, c.Value)
                sessionsMu.Unlock()
            }
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach username via context? For simplicity, set header for handler
        r.Header.Set("X-Auth-Username", sess.Username)
        r.Header.Set("X-Auth-CSRF", sess.CSRFToken)

        // refresh expiry (sliding window)
        sess.Expires = time.Now().Add(sessionTTL)
        sessionsMu.Lock()
        sessions[c.Value] = sess
        sessionsMu.Unlock()

        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    writeSecurityHeaders(w)
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    writeSecurityHeaders(w)
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="64" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    writeSecurityHeaders(w)
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic input canonicalization and validation
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")
    if username == "" || len(username) > maxInputLen || len(password) > maxInputLen {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // For demo, only 'admin' exists. Compare username and bcrypt password using constant-time compare.
    if subtle.ConstantTimeCompare([]byte(username), []byte("admin")) == 1 {
        // verify password
        if bcrypt.CompareHashAndPassword(adminHash, []byte(password)) == nil {
            // create session
            sessionID, err := randomBase64(32)
            if err != nil {
                http.Error(w, "Internal error", http.StatusInternalServerError)
                return
            }
            csrfToken, err := randomBase64(32)
            if err != nil {
                http.Error(w, "Internal error", http.StatusInternalServerError)
                return
            }
            expires := time.Now().Add(sessionTTL)
            sessionsMu.Lock()
            sessions[sessionID] = sessionData{
                Username:  username,
                Expires:   expires,
                CSRFToken: csrfToken,
            }
            sessionsMu.Unlock()

            // set cookie with secure attributes; Secure flag set when TLS is used
            cookie := &http.Cookie{
                Name:     sessionCookieName,
                Value:    sessionID,
                Path:     "/",
                Expires:  expires,
                HttpOnly: true,
                SameSite: http.SameSiteStrictMode,
                Secure:   r.TLS != nil, // set secure only if TLS is present
            }
            http.SetCookie(w, cookie)
            http.Redirect(w, r, "/settings", http.StatusFound)
            return
        }
    }

    // generic error message to avoid username enumeration
    fmt.Fprint(w, "Bad username or password")
}

// user settings page (protected by ensureLoggedIn)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    writeSecurityHeaders(w)

    // Retrieve authenticated username and CSRF token from request headers set in ensureLoggedIn
    username := r.Header.Get("X-Auth-Username")
    csrfToken := r.Header.Get("X-Auth-CSRF")
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Render a minimal template with escaped username and a logout form including CSRF token.
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
          <head><meta charset="utf-8"><title>Settings</title></head>
          <body>
            <h1>Secret settings for {{.Username}}</h1>
            <p>Only visible to authenticated users.</p>
            <form action="/logout" method="post">
              <input type="hidden" name="csrf_token" value="{{.CSRF}}">
              <button type="submit">Logout</button>
            </form>
          </body>
        </html>
    `))

    data := struct {
        Username string
        CSRF     string
    }{
        Username: username,
        CSRF:     csrfToken,
    }
    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Render error", http.StatusInternalServerError)
        return
    }
}

// logout handler: terminates session after verifying CSRF token
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    writeSecurityHeaders(w)
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // get cookie
    c, err := r.Cookie(sessionCookieName)
    if err != nil || c.Value == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // get session
    sessionsMu.RLock()
    sess, ok := sessions[c.Value]
    sessionsMu.RUnlock()
    if !ok {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // verify CSRF token from form in constant time
    formToken := r.FormValue("csrf_token")
    if subtle.ConstantTimeCompare([]byte(formToken), []byte(sess.CSRFToken)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }

    // delete session
    sessionsMu.Lock()
    delete(sessions, c.Value)
    sessionsMu.Unlock()

    // expire cookie
    expired := &http.Cookie{
        Name:     sessionCookieName,
        Value:    "",
        Path:     "/",
        Expires:  time.Unix(0, 0),
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        Secure:   r.TLS != nil,
    }
    http.SetCookie(w, expired)
    http.Redirect(w, r, "/", http.StatusFound)
}

// writeSecurityHeaders applies common web hardening headers
func writeSecurityHeaders(w http.ResponseWriter) {
    // Content-Security-Policy: restrict to self; adjust as needed
    w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none'")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // HSTS should be enabled only when TLS is in use in production
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
}

// randomBase64 returns a URL-safe base64 string of length ~n*4/3
func randomBase64(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}
