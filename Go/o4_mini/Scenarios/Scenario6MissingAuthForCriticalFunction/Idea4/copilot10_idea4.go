package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "encoding/base64"
    "encoding/json"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
var (
    // secrets loaded from environment at startup
    sessionKey         []byte
    adminPasswordHash  []byte
    secureCookieFlag   = true // can be disabled for local non-TLS testing by setting DISABLE_SECURE_COOKIE=1
    sessionCookieName  = "session"
    csrfCookieName     = "csrf"
    sessionExpiryHours = 1
)

// ...existing code...
func main() {
    // load secrets
    if os.Getenv("DISABLE_SECURE_COOKIE") == "1" {
        secureCookieFlag = false
    }

    sk := os.Getenv("SESSION_KEY")
    if sk == "" {
        log.Fatal("SESSION_KEY environment variable is required")
    }
    sessionKey = []byte(sk)

    ah := os.Getenv("ADMIN_PASSWORD_HASH")
    if ah == "" {
        log.Fatal("ADMIN_PASSWORD_HASH environment variable is required (bcrypt hash of admin password)")
    }
    adminPasswordHash = []byte(ah)

    http.HandleFunc("/", secureHeaders(indexHandler))
    http.HandleFunc("/login", secureHeaders(loginHandler))
    // POST must validate CSRF -> wrap with CSRF validation plus secure headers
    http.HandleFunc("/login_post", secureHeaders(csrfProtect(doLoginHandler)))
    // protect settings with authentication middleware and secure headers
    http.HandleFunc("/settings", secureHeaders(ensureLoggedIn(settingsHandler)))
    addr := ":8080"
    log.Printf("Starting server on %s", addr)
    // In production use ListenAndServeTLS with proper certs
    http.ListenAndServe(addr, nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username, err := validateSessionCookie(r)
        if err != nil || username == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username to request context if needed (omitted for brevity)
        next(w, r)
    }
}

// secureHeaders adds common security response headers
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Content Security Policy: restrict to self; adjust as needed
        w.Header().Set("Content-Security-Policy", "default-src 'self'; object-src 'none'; frame-ancestors 'none';")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "no-referrer")
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        w.Header().Set("X-XSS-Protection", "1; mode=block")
        next(w, r)
    }
}

// helper: create signed session cookie
func createSessionCookie(w http.ResponseWriter, username string) error {
    type payload struct {
        User   string `json:"u"`
        Expiry int64  `json:"e"`
    }
    p := payload{
        User:   username,
        Expiry: time.Now().Add(time.Duration(sessionExpiryHours) * time.Hour).Unix(),
    }
    b, err := json.Marshal(p)
    if err != nil {
        return err
    }
    sig := sign(b)
    val := base64.RawURLEncoding.EncodeToString(b) + "." + base64.RawURLEncoding.EncodeToString(sig)
    c := &http.Cookie{
        Name:     sessionCookieName,
        Value:    val,
        Path:     "/",
        HttpOnly: true,
        Secure:   secureCookieFlag,
        SameSite: http.SameSiteStrictMode,
        MaxAge:   sessionExpiryHours * 3600,
    }
    http.SetCookie(w, c)
    return nil
}

// validateSessionCookie returns username or error
func validateSessionCookie(r *http.Request) (string, error) {
    c, err := r.Cookie(sessionCookieName)
    if err != nil {
        return "", err
    }
    parts := strings.SplitN(c.Value, ".", 2)
    if len(parts) != 2 {
        return "", fmt.Errorf("invalid cookie format")
    }
    payloadB, err := base64.RawURLEncoding.DecodeString(parts[0])
    if err != nil {
        return "", err
    }
    sigB, err := base64.RawURLEncoding.DecodeString(parts[1])
    if err != nil {
        return "", err
    }
    if !verify(payloadB, sigB) {
        return "", fmt.Errorf("invalid signature")
    }
    var p struct {
        User   string `json:"u"`
        Expiry int64  `json:"e"`
    }
    if err := json.Unmarshal(payloadB, &p); err != nil {
        return "", err
    }
    if time.Now().Unix() > p.Expiry {
        return "", fmt.Errorf("session expired")
    }
    // basic validation: username whitelist / canonicalization
    if p.User == "" || len(p.User) > 64 {
        return "", fmt.Errorf("invalid username")
    }
    return p.User, nil
}

func sign(msg []byte) []byte {
    h := hmac.New(sha256.New, sessionKey)
    h.Write(msg)
    return h.Sum(nil)
}
func verify(msg, sig []byte) bool {
    expected := sign(msg)
    // constant time compare
    return hmac.Equal(expected, sig)
}

// generateCSRFToken creates a secure random token, sets a cookie (HttpOnly is true),
// and also returns the token so handlers can embed it in forms.
func generateCSRFToken(w http.ResponseWriter) (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    token := base64.RawURLEncoding.EncodeToString(b)
    c := &http.Cookie{
        Name:     csrfCookieName,
        Value:    token,
        Path:     "/",
        HttpOnly: true, // not accessible to JS; we embed the token server-side in the form
        Secure:   secureCookieFlag,
        SameSite: http.SameSiteStrictMode,
        MaxAge:   300,
    }
    http.SetCookie(w, c)
    return token, nil
}

// csrfProtect validates double-submit token for POST handlers
func csrfProtect(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        if r.Method == http.MethodPost {
            formToken := r.PostFormValue("csrf_token")
            c, err := r.Cookie(csrfCookieName)
            if err != nil || formToken == "" || c.Value == "" || !hmac.Equal([]byte(formToken), []byte(c.Value)) {
                http.Error(w, "Invalid CSRF token", http.StatusForbidden)
                return
            }
        }
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // minimal safe output
    fmt.Fprint(w, `<html>Hello! <a href="/login">Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // generate CSRF token and embed in form
    token, err := generateCSRFToken(w)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    tpl := template.Must(template.New("login").Parse(`
        <html>
        <body>
        <form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="64" autocomplete="username"></p>
            <p><input type="password" name="password" maxlength="128" autocomplete="current-password"></p>
            <input type="hidden" name="csrf_token" value="{{.CSRF}}">
            <p><button type="submit">Submit</button></p>
        </form>
        </body>
        </html>
    `))
    _ = tpl.Execute(w, map[string]string{"CSRF": token})
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // minimal input validation / canonicalization
    username := strings.TrimSpace(r.PostFormValue("username"))
    password := r.PostFormValue("password")
    if username == "" || len(username) > 64 || password == "" {
        http.Error(w, "Bad username or password", http.StatusBadRequest)
        return
    }
    // whitelist allowed username(s) for this demo: canonicalize to lowercase
    username = strings.ToLower(username)

    // Verify password using bcrypt (hash provided via env ADMIN_PASSWORD_HASH)
    if err := bcrypt.CompareHashAndPassword(adminPasswordHash, []byte(password)); err != nil {
        // Do not reveal which part failed
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // create signed session cookie
    if err := createSessionCookie(w, username); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // validate session again and display only minimal info
    username, err := validateSessionCookie(r)
    if err != nil {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    // keep output escaped using template
    t := template.Must(template.New("settings").Parse(`<html><body><h1>Settings for {{.User}}</h1><p>Secret settings</p></body></html>`))
    _ = t.Execute(w, map[string]string{"User": username})
}
