// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "regexp"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    // secure session store: token -> username
    sessions   = make(map[string]string)
    sessionsMu sync.RWMutex

    // pre-hashed admin password (for demo). In production store hashed passwords in a DB.
    adminHash []byte

    // safe template for settings page (auto-escaped)
    settingsTmpl = template.Must(template.New("settings").Parse(`
<html>
  <head><meta charset="utf-8"><title>Settings</title></head>
  <body>
    <h1>Secret settings</h1>
    <p>User: {{.UserMasked}}</p>
    <!-- Do not include any sensitive data -->
  </body>
</html>
`))
)

func init() {
    // Hash the demo password once at startup; do not store plaintext in real apps.
    var err error
    adminHash, err = bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to generate password hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)

    // protect settings route with authentication wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    // start server (for production use TLS; here we use HTTP for local testing)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ensureLoggedIn decorator equivalent: validates secure session cookie and injects username into request context
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_token")
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // Validate token format (hex, expected length)
        if len(cookie.Value) != 64 { // 32 bytes -> 64 hex chars
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        if _, err := hex.DecodeString(cookie.Value); err != nil {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // look up session safely
        sessionsMu.RLock()
        username, ok := sessions[cookie.Value]
        sessionsMu.RUnlock()
        if !ok || username == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // inject username into context for the downstream handler (avoid exposing raw data to templates)
        ctx := context.WithValue(r.Context(), "username", username)
        next(w, r.WithContext(ctx))
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // static HTML is safe here
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // simple safe form (no untrusted values echoed back)
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="32" autocomplete="username"></p>
            <p><input type="password" name="password" maxlength="128" autocomplete="current-password"></p>
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

    // Parse form data with limits
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate username: allow only alphanumeric up to 32 chars to avoid unexpected input
    if !validUsername(username) || len(password) == 0 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // For demo only: only "admin" user exists. Compare hashed password.
    if username == "admin" {
        if err := bcrypt.CompareHashAndPassword(adminHash, []byte(password)); err != nil {
            // Do not reveal which part failed
            http.Error(w, "Bad username or password", http.StatusUnauthorized)
            return
        }

        // create secure random session token
        token, err := newSessionToken(32)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }

        // store session
        sessionsMu.Lock()
        sessions[token] = username
        sessionsMu.Unlock()

        // set cookie: HttpOnly and SameSite to mitigate XSS/CSRF; Secure should be true in production with HTTPS
        http.SetCookie(w, &http.Cookie{
            Name:     "session_token",
            Value:    token,
            Path:     "/",
            HttpOnly: true,
            Secure:   false, // set to true in production (HTTPS)
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(24 * time.Hour),
        })

        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    // generic error for any other user to avoid user enumeration
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// user settings page (requires authentication via ensureLoggedIn)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // retrieve username from context (set by ensureLoggedIn)
    username, _ := r.Context().Value("username").(string)
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // mask username before display to avoid exposing sensitive info
    userMasked := maskUsername(username)

    // render safe template (html/template auto-escapes)
    data := struct {
        UserMasked string
    }{
        UserMasked: userMasked,
    }
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
}

// helper: create cryptographically secure random hex token of n bytes
func newSessionToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// helper: validate username (alphanumeric, 1-32 chars)
var usernameRegexp = regexp.MustCompile(`^[A-Za-z0-9]{1,32}$`)

func validUsername(u string) bool {
    return usernameRegexp.MatchString(u)
}

// helper: mask username for display (first char + ****)
func maskUsername(u string) string {
    r := []rune(u)
    if len(r) == 0 {
        return ""
    }
    if len(r) == 1 {
        return string(r[0]) + "****"
    }
    return string(r[0]) + "****" + string(r[len(r)-1])
}

// ...existing code...
