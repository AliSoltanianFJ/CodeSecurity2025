package main

import (
    "crypto/rand"
    "encoding/base64"
    "fmt"
    "log"
    "net/http"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// simple in-memory session store (token -> username + expiry)
type sessionData struct {
    username string
    expiry   time.Time
}

var (
    sessions      = make(map[string]sessionData)
    sessionsMutex sync.RWMutex
    // store a hashed password instead of plaintext
    hashedPassword []byte
)

func init() {
    // hash the example password at startup (in real apps store and load a hash from secure storage)
    h, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to hash password: %v", err)
    }
    hashedPassword = h
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with middleware that checks a session cookie
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_token")
        if err != nil {
            // no cookie -> not authenticated
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        token := cookie.Value

        sessionsMutex.RLock()
        sess, ok := sessions[token]
        sessionsMutex.RUnlock()

        if !ok || time.Now().After(sess.expiry) {
            // invalid or expired session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // valid session; continue
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

// generate a secure random session token
func generateSessionToken() (string, error) {
    b := make([]byte, 32)
    _, err := rand.Read(b)
    if err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // simple authentication check using bcrypt
    if username == "admin" && bcrypt.CompareHashAndPassword(hashedPassword, []byte(password)) == nil {
        token, err := generateSessionToken()
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        expiry := time.Now().Add(30 * time.Minute)

        sessionsMutex.Lock()
        sessions[token] = sessionData{username: username, expiry: expiry}
        sessionsMutex.Unlock()

        // set a secure cookie (HttpOnly). Set Secure: true when using TLS.
        http.SetCookie(w, &http.Cookie{
            Name:     "session_token",
            Value:    token,
            Path:     "/",
            Expires:  expiry,
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
            // Secure:   true, // enable when serving over HTTPS
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // At this point ensureLoggedIn already validated the session cookie.
    // Retrieve username from session store to personalize the page.
    cookie, err := r.Cookie("session_token")
    if err != nil {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    token := cookie.Value

    sessionsMutex.RLock()
    sess, ok := sessions[token]
    sessionsMutex.RUnlock()

    if !ok || time.Now().After(sess.expiry) {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Example settings page — sensitive content only returned to authenticated users.
    fmt.Fprintf(w, "<html>Secret settings for %s</html>", sess.username)
}
