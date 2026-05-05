package main

import (
    "context"
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "html/template"
    "io"
    "log"
    "net/http"
    "sync"
    "time"
)

const (
    sessionCookieName = "sid"
    csrfContextKey    = "csrfToken"
    sessionContextKey = "session"
)

type Session struct {
    Username  string
    CSRFToken string
}

var (
    // in-memory session store; swap for your datastore of choice
    sessions = struct {
        sync.RWMutex
        m map[string]*Session
    }{m: make(map[string]*Session)}

    // per-user settings store (in this example we only track “Email”)
    userSettings = struct {
        sync.RWMutex
        m map[string]string
    }{m: make(map[string]string)}
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)

    // wrap settingsHandler in ensureLoggedIn
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// generate a secure random string of length 2*n (hex)
func randomHex(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// create a new session for this user, set cookie, return sessionID
func createSession(w http.ResponseWriter, username string) (string, error) {
    sid, err := randomHex(32)
    if err != nil {
        return "", err
    }
    csrf, err := randomHex(32)
    if err != nil {
        return "", err
    }
    sessions.Lock()
    sessions.m[sid] = &Session{
        Username:  username,
        CSRFToken: csrf,
    }
    sessions.Unlock()

    cookie := &http.Cookie{
        Name:     sessionCookieName,
        Value:    sid,
        Path:     "/",
        HttpOnly: true,
        Secure:   false,      // set to true if you serve over HTTPS
        Expires:  time.Now().Add(24 * time.Hour),
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, cookie)
    return sid, nil
}

// retrieve session from request cookie (or nil)
func getSession(r *http.Request) (*Session, string) {
    c, err := r.Cookie(sessionCookieName)
    if err != nil {
        return nil, ""
    }
    sid := c.Value
    sessions.RLock()
    sess := sessions.m[sid]
    sessions.RUnlock()
    return sess, sid
}

// decorator that forces login and injects *Session into request context
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        sess, sid := getSession(r)
        if sess == nil {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // inject session into context
        ctx := context.WithValue(r.Context(), sessionContextKey, sess)
        next(w, r.WithContext(ctx))
        // (optional) if CSRF token rotated in handler, write it back to store
        if newCSRF, ok := r.Context().Value(csrfContextKey).(string); ok && newCSRF != sess.CSRFToken {
            sessions.Lock()
            if s, stillThere := sessions.m[sid]; stillThere {
                s.CSRFToken = newCSRF
            }
            sessions.Unlock()
        }
    }
}

// GET /login
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    io.WriteString(w, `
    <html>
    <form action="/login_post" method="post">
      <p><input type="text" name="username" placeholder="username"></p>
      <p><input type="password" name="password" placeholder="password"></p>
      <p><button type="submit">Login</button></p>
    </form>
    </html>`)
}

// POST /login_post
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // TODO: replace with real password check
    if username == "admin" && password == "password" {
        if _, err := createSession(w, username); err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    http.Error(w, "Invalid credentials", http.StatusUnauthorized)
}

// settingsHandler handles both GET and POST to /settings
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // pull session from context
    ctxSess := r.Context().Value(sessionContextKey)
    if ctxSess == nil {
        http.Error(w, "Not logged in", http.StatusUnauthorized)
        return
    }
    sess := ctxSess.(*Session)

    switch r.Method {
    case http.MethodGet:
        // render the settings form, including current email and CSRF token
        userSettings.RLock()
        currentEmail := userSettings.m[sess.Username]
        userSettings.RUnlock()

        tmpl := `
        <html>
        <body>
        <h1>Settings for {{.User}}</h1>
        <form method="post" action="/settings">
          <input type="hidden" name="csrf_token" value="{{.CSRF}}">
          <p>Email: <input type="email" name="email" value="{{.Email}}"></p>
          <p><button type="submit">Save</button></p>
        </form>
        </body>
        </html>
        `
        t := template.Must(template.New("settings").Parse(tmpl))
        data := struct {
            User  string
            Email string
            CSRF  string
        }{
            User:  sess.Username,
            Email: currentEmail,
            CSRF:  sess.CSRFToken,
        }
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        t.Execute(w, data)

    case http.MethodPost:
        // check CSRF token
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Bad request", http.StatusBadRequest)
            return
        }
        token := r.FormValue("csrf_token")
        if token == "" || token != sess.CSRFToken {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }

        // grab and sanitize input
        newEmail := r.FormValue("email")
        // here you’d do real validation/sanitization
        if len(newEmail) > 254 {
            http.Error(w, "Email too long", http.StatusBadRequest)
            return
        }

        // persist
        userSettings.Lock()
        userSettings.m[sess.Username] = newEmail
        userSettings.Unlock()

        // rotate CSRF token (single-use)
        newCSRF, err := randomHex(32)
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        // propagate back into session via context
        r = r.WithContext(context.WithValue(r.Context(), csrfContextKey, newCSRF))

        // redirect back to GET so the user sees updated settings
        http.Redirect(w, r, "/settings", http.StatusSeeOther)

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

// simple home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href="/login">Login here</a></html>`)
}