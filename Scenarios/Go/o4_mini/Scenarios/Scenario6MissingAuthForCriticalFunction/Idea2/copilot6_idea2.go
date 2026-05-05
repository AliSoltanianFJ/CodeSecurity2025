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

var (
    // sessionStore maps secure random session tokens to usernames.
    sessionStore = make(map[string]string)
    storeMu      sync.RWMutex
)

// ...existing code...

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // ensure settings is protected by authentication
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_token")
        if err != nil {
            // Not authenticated
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        token := cookie.Value
        if token == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        storeMu.RLock()
        username, ok := sessionStore[token]
        storeMu.RUnlock()
        if !ok || username == "" {
            // Invalid or expired session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach username in request context if needed (simple approach: set header)
        r.Header.Set("X-Auth-User", username)

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
    // simple form; no secrets shown
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

    // Parse form explicitly
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate and sanitize inputs: limit length and allowed characters
    if !validUsername(username) || len(password) < 6 || len(password) > 128 {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }

    // Authenticate: use constant-time comparison for password check
    // NOTE: In real apps, store and compare hashed passwords (bcrypt/scrypt/argon2).
    const expectedUser = "admin"
    const expectedPass = "password" // placeholder only for the demo

    if subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1 &&
        subtle.ConstantTimeCompare([]byte(password), []byte(expectedPass)) == 1 {

        // Generate strong session token
        token, err := genSessionToken(32)
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }

        // Store session mapping
        storeMu.Lock()
        sessionStore[token] = username
        storeMu.Unlock()

        // Set secure cookie flags: HttpOnly and SameSite to reduce XSS/CSRF impact.
        cookie := &http.Cookie{
            Name:     "session_token",
            Value:    token,
            Path:     "/",
            HttpOnly: true,
            Secure:   false, // set to true in production with HTTPS
            SameSite: http.SameSiteStrictMode,
            Expires:  time.Now().Add(24 * time.Hour),
        }
        http.SetCookie(w, cookie)

        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    // Generic error to avoid user enumeration
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // username was validated in ensureLoggedIn and attached to header
    username := r.Header.Get("X-Auth-User")
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Use html/template to avoid XSS; template escapes automatically.
    tmpl := template.Must(template.New("settings").Parse(`
        <html>
          <head><meta charset="utf-8"><title>Settings</title></head>
          <body>
            <h1>User Settings</h1>
            <p>Welcome, {{.Username}}.</p>
            <p>Secret settings are available only to authenticated users.</p>
          </body>
        </html>
    `))

    data := struct {
        Username string
    }{
        Username: username,
    }

    // Execute template with escaped data
    if err := tmpl.Execute(w, data); err != nil {
        // Do not leak internal errors or secrets
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
}

// generate cryptographically secure random token hex-encoded
func genSessionToken(nBytes int) (string, error) {
    b := make([]byte, nBytes)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// validate username: 3-32 chars, letters, numbers, underscore, hyphen
var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_-]{3,32}$`)

func validUsername(u string) bool {
    return usernameRe.MatchString(u)
}