package main

import (
    "crypto/rand"
    "encoding/hex"
    "html/template"
    "io"
    "log"
    "net/http"
    "sync"
    "time"
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Wrap settingsHandler in our authentication middleware:
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("Listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ----------------------
// SESSION MANAGEMENT
// ----------------------

// Session holds per‐user data (username + CSRF token).
type Session struct {
    Username  string
    CSRFTOKEN string
    // you could also store other per‐user things here
}

// sessions is our in‐memory session store.
// In production you’d back this with Redis or a database.
var (
    sessions   = make(map[string]*Session)
    sessionsMu sync.RWMutex
)

// newSessionID generates a secure random 32-byte hex string.
func newSessionID() string {
    b := make([]byte, 32)
    if _, err := io.ReadFull(rand.Reader, b); err != nil {
        panic("unable to generate session ID: " + err.Error())
    }
    return hex.EncodeToString(b)
}

// newCSRFToken generates a secure random 32-byte hex string.
func newCSRFToken() string {
    return newSessionID()
}

// getSession returns the session object (if any) for this request.
// If there is no valid session cookie, it returns nil.
func getSession(r *http.Request) *Session {
    c, err := r.Cookie("session_id")
    if err != nil {
        return nil
    }
    sessionsMu.RLock()
    defer sessionsMu.RUnlock()
    return sessions[c.Value]
}

// setSession creates a new session and sets the cookie.
func setSession(w http.ResponseWriter, username string) {
    sid := newSessionID()
    csrf := newCSRFToken()

    sessionsMu.Lock()
    sessions[sid] = &Session{
        Username:  username,
        CSRFTOKEN: csrf,
    }
    sessionsMu.Unlock()

    // Set a secure cookie.
    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sid,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true if you’re on HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)
}

// clearSession removes a session (e.g. on logout).
func clearSession(w http.ResponseWriter, r *http.Request) {
    if c, err := r.Cookie("session_id"); err == nil {
        sessionsMu.Lock()
        delete(sessions, c.Value)
        sessionsMu.Unlock()

        // Overwrite client cookie
        c.Value = ""
        c.Expires = time.Unix(0, 0)
        http.SetCookie(w, c)
    }
}

// ----------------------
// AUTH MIDDLEWARE
// ----------------------

// ensureLoggedIn is our “decorator” for protected endpoints.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        sess := getSession(r)
        if sess == nil || sess.Username == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // Put the session into the request context if you like.
        next(w, r)
    }
}

// ----------------------
// HANDLERS
// ----------------------

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`<html>
        <body>
          Hello! <a href="/login">Login here</a>
        </body>
      </html>`))
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`
      <html>
      <body>
        <form action="/login_post" method="post">
          <p><input type="text" name="username" placeholder="username"></p>
          <p><input type="password" name="password" placeholder="password"></p>
          <p><button type="submit">Submit</button></p>
        </form>
      </body>
      </html>
    `))
}

func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // In real life you’d check this against a hashed password in your DB.
    if username == "admin" && password == "password" {
        setSession(w, username)
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusUnauthorized)
    w.Write([]byte("Bad username or password"))
}

// ----------------------
// SETTINGS HANDLER
// ----------------------

// pretend we store a single preference per user in memory
var (
    userPrefs   = make(map[string]string)
    userPrefsMu sync.RWMutex
)

var settingsTmpl = template.Must(template.New("settings").Parse(`
<html>
  <head><title>Settings</title></head>
  <body>
    <h1>Settings for {{.Username}}</h1>
    {{if .Flash}}
      <div style="color:green">{{.Flash}}</div>
    {{end}}
    <form action="/settings" method="post">
      <input type="hidden" name="csrf_token" value="{{.CSRFToken}}">
      <p>Your favorite color:
         <input type="text" name="color" value="{{.Preference}}">
      </p>
      <button type="submit">Save</button>
    </form>
    <p><a href="/">Home</a></p>
  </body>
</html>
`))

func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // We already know the user is logged in.
    sess := getSession(r)
    if sess == nil {
        // should never happen, but just in case:
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    switch r.Method {
    case http.MethodGet:
        renderSettingsForm(w, sess, "")

    case http.MethodPost:
        // 1) CSRF check
        formToken := r.FormValue("csrf_token")
        if formToken == "" || formToken != sess.CSRFTOKEN {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }

        // 2) Read & sanitize user input
        color := r.FormValue("color")
        if len(color) > 100 {
            http.Error(w, "Input too long", http.StatusBadRequest)
            return
        }

        // 3) Store preference
        userPrefsMu.Lock()
        userPrefs[sess.Username] = color
        userPrefsMu.Unlock()

        // 4) Rotate CSRF token on every successful POST
        newToken := newCSRFToken()
        sessionsMu.Lock()
        sess.CSRFTOKEN = newToken
        sessionsMu.Unlock()

        // 5) Re-render the form with a “flash” message
        renderSettingsForm(w, sess, "Saved successfully!")

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

func renderSettingsForm(w http.ResponseWriter, sess *Session, flash string) {
    // grab existing preference (if any)
    userPrefsMu.RLock()
    pref := userPrefs[sess.Username]
    userPrefsMu.RUnlock()

    data := struct {
        Username   string
        CSRFToken  string
        Preference string
        Flash      string
    }{
        Username:   sess.Username,
        CSRFToken:  sess.CSRFTOKEN,
        Preference: pref,
        Flash:      flash,
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
    }
}