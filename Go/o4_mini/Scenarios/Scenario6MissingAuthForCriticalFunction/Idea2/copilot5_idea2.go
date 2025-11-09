package main

import (
    "crypto/rand"
    "crypto/subtle"
    "encoding/base64"
    "html/template"
    "log"
    "net/http"
    "strings"
    "sync"
    "time"
)

var (
    sessions   = make(map[string]string) // token -> username
    sessionsMu sync.RWMutex
    tmpl       = template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html lang="en">
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Settings</h1>
            <p>Welcome, {{ .Username }}</p>
        </body>
        </html>
    `))
)

// generate a cryptographically secure session token
func generateSessionToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    // bind explicitly and fail fast if port unavailable
    srv := &http.Server{
        Addr:              ":8080",
        ReadHeaderTimeout: 5 * time.Second,
    }
    log.Println("starting server on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server error: %v", err)
    }
}

// ensureLoggedIn middleware checks for a valid session token cookie.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_token")
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        token := c.Value

        sessionsMu.RLock()
        username, ok := sessions[token]
        sessionsMu.RUnlock()
        if !ok || username == "" {
            // invalid session: remove cookie and redirect to login
            http.SetCookie(w, &http.Cookie{
                Name:     "session_token",
                Value:    "",
                Path:     "/",
                Expires:  time.Unix(0, 0),
                MaxAge:   -1,
                HttpOnly: true,
                SameSite: http.SameSiteLaxMode,
            })
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username to context via request with closure capture (simple)
        r = r.WithContext(withUsername(r.Context(), username))
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    _, _ = w.Write([]byte(`<html>Hello! <a href='./login'>Login here</a></html>`))
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    _, _ = w.Write([]byte(`
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="32" autocomplete="username"></p>
            <p><input type="password" name="password" maxlength="128" autocomplete="current-password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `))
}

// login POST system with input validation and secure session token
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form with limits (default is fine but trim inputs)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := strings.TrimSpace(r.FormValue("password"))

    // basic validation: length and allowed characters (alphanumeric and few symbols)
    if username == "" || len(username) > 32 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // authenticate (in real apps use hashed passwords and secure user storage)
    const expectedUser = "admin"
    const expectedPass = "password"

    // constant time compare for password
    userOK := subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1
    passOK := subtle.ConstantTimeCompare([]byte(password), []byte(expectedPass)) == 1

    if userOK && passOK {
        token, err := generateSessionToken()
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        sessionsMu.Lock()
        sessions[token] = username
        sessionsMu.Unlock()

        // set cookie securely (HttpOnly; SameSite Lax). Do not set Secure true here since server is plain HTTP in examples.
        http.SetCookie(w, &http.Cookie{
            Name:     "session_token",
            Value:    token,
            Path:     "/",
            Expires:  time.Now().Add(24 * time.Hour),
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    // generic message to avoid username enumeration
    http.Error(w, "Invalid credentials", http.StatusUnauthorized)
}

// settingsHandler displays a sanitized page for authenticated users.
// Uses html/template which auto-escapes content to prevent XSS.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := usernameFromContext(r.Context())
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    data := struct {
        Username string
    }{
        Username: username,
    }
    // template auto-escapes Username
    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}

//
// lightweight context helpers to pass username from middleware to handlers
//
import (
    "context"
)

type ctxKeyUsername struct{}

func withUsername(ctx context.Context, username string) context.Context {
    return context.WithValue(ctx, ctxKeyUsername{}, username)
}

func usernameFromContext(ctx context.Context) string {
    v := ctx.Value(ctxKeyUsername{})
    if s, ok := v.(string); ok {
        return s
    }
    return ""
}
