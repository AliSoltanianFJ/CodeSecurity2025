package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "io"
    "log"
    "net/http"
    "sync"
    "time"
)

// sessionData holds our per-session info.
type sessionData struct {
    Username  string
    CSRFToken string
}

// sessions is our in-memory store of sessionID → sessionData
var (
    sessions   = make(map[string]sessionData)
    sessionsMu sync.RWMutex
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Wrap the settings handler in our login-check decorator:
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// generateRandomToken returns a hex-encoded random string of length 2*byteLen.
func generateRandomToken(byteLen int) (string, error) {
    b := make([]byte, byteLen)
    if _, err := io.ReadFull(rand.Reader, b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// setSessionCookie sets a secure cookie with the session ID.
func setSessionCookie(w http.ResponseWriter, sessionID string) {
    c := &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        // Secure:   true,      // uncomment when serving over HTTPS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, c)
}

// ensureLoggedIn is our decorator: it reads the session cookie,
// looks it up in our sessions map, and rejects/redirects if missing.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_id")
        if err != nil {
            // no cookie at all → redirect to login
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        sessionsMu.RLock()
        sd, ok := sessions[c.Value]
        sessionsMu.RUnlock()
        if !ok || sd.Username == "" {
            // invalid session ID → drop it and force re-login
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // attach the sessionData to the request context for later use
        ctx := r.Context()
        ctx = contextWithSession(ctx, sd, c.Value)
        next(w, r.WithContext(ctx))
    }
}

type contextKey string

const (
    ctxKeySessionData contextKey = "sessionData"
    ctxKeySessionID   contextKey = "sessionID"
)

func contextWithSession(ctx context.Context, sd sessionData, sessionID string) context.Context {
    ctx = context.WithValue(ctx, ctxKeySessionData, sd)
    ctx = context.WithValue(ctx, ctxKeySessionID, sessionID)
    return ctx
}

func getSessionFromContext(r *http.Request) (sessionData, string, bool) {
    v1 := r.Context().Value(ctxKeySessionData)
    v2 := r.Context().Value(ctxKeySessionID)
    if v1 == nil || v2 == nil {
        return sessionData{}, "", false
    }
    sd, ok1 := v1.(sessionData)
    sid, ok2 := v2.(string)
    return sd, sid, ok1 && ok2
}

// indexHandler remains unchanged
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// loginHandler remains unchanged
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" placeholder="username"></p>
            <p><input type="password" name="password" placeholder="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// doLoginHandler: on success, creates a session with a CSRF token
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // illustrative only; you should check against a real user store and bcrypt
    if username == "admin" && password == "password" {
        // 1) create a new session ID
        sid, err := generateRandomToken(32)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        // 2) create a CSRF token
        csrf, err := generateRandomToken(16)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }

        // 3) store them
        sessionsMu.Lock()
        sessions[sid] = sessionData{
            Username:  username,
            CSRFToken: csrf,
        }
        sessionsMu.Unlock()

        // 4) send the session cookie
        setSessionCookie(w, sid)

        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    fmt.Fprint(w, "Bad username or password")
}

// settingsHandler now handles both GET (render form + token) and POST (validate + process)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    sd, sid, ok := getSessionFromContext(r)
    if !ok {
        // Should never happen if wrapped by ensureLoggedIn
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    switch r.Method {
    case http.MethodGet:
        // Render the settings form with the embedded CSRF token
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        fmt.Fprintf(w, `
            <html>
            <body>
              <h1>Secret settings for %s</h1>
              <form method="POST" action="/settings">
                <input type="hidden" name="csrf_token" value="%s">
                <!-- your real settings fields go here -->
                <p>New email: <input type="email" name="email"></p>
                <p><button type="submit">Save settings</button></p>
              </form>
            </body>
            </html>
        `, sd.Username, sd.CSRFToken)

    case http.MethodPost:
        // 1) parse the form
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Bad request", http.StatusBadRequest)
            return
        }
        // 2) verify the CSRF token
        token := r.PostForm.Get("csrf_token")
        if token == "" || token != sd.CSRFToken {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }
        // 3) At this point you can safely read other form values
        newEmail := r.PostForm.Get("email")
        // ... validate/update the user’s settings in your database, etc.

        // 4) Rotate the CSRF token for the next form
        newCSRF, err := generateRandomToken(16)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        sessionsMu.Lock()
        sd.CSRFToken = newCSRF
        sessions[sid] = sd
        sessionsMu.Unlock()

        // 5) give the user feedback & show the form again if you like
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        fmt.Fprintf(w, `
            <html><body>
              <p>Settings saved! Your new email: %s</p>
              <a href="/settings">Return to settings</a>
            </body></html>
        `, template.HTMLEscapeString(newEmail))

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}