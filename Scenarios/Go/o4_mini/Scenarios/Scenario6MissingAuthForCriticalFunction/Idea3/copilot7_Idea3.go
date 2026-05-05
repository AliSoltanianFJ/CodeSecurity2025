package main
import (
    "crypto/rand"
    "encoding/base64"
    "html/template"
    "log"
    "net/http"
    "strings"
    "sync"
    "time"
)

// Session holds our per-user state.
type Session struct {
    Username  string
    CSRFToken string
    Created   time.Time
}

// sessions is our in-memory session store.
// In production you’d back this by Redis, a DB, etc.
var (
    sessions   = map[string]*Session{}
    sessionsMu sync.RWMutex
)

// randToken returns a URL-safe, base64 encoded random string of length n.
func randToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// setSession creates a new session for username, stores it server-side,
// and sets a secure cookie on the ResponseWriter.
func setSession(w http.ResponseWriter, username string) error {
    // 1) generate a new session ID
    sid, err := randToken(32)
    if err != nil {
        return err
    }

    // 2) generate a CSRF token
    csrf, err := randToken(32)
    if err != nil {
        return err
    }

    // 3) store it
    sessionsMu.Lock()
    sessions[sid] = &Session{
        Username:  username,
        CSRFToken: csrf,
        Created:   time.Now(),
    }
    sessionsMu.Unlock()

    // 4) set a cookie
    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sid,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // <-- set to true if you are on HTTPS
        SameSite: http.SameSiteLaxMode,
        // you can also set cookie.MaxAge or Expires here
    }
    http.SetCookie(w, cookie)
    return nil
}

// getSession reads the session_id cookie, looks it up, and returns the Session.
func getSession(r *http.Request) (*Session, error) {
    c, err := r.Cookie("session_id")
    if err != nil {
        return nil, err
    }
    sessionsMu.RLock()
    defer sessionsMu.RUnlock()
    sess, ok := sessions[c.Value]
    if !ok {
        return nil, http.ErrNoCookie
    }
    return sess, nil
}

// clearSession deletes the session server-side and clears the cookie.
func clearSession(w http.ResponseWriter, r *http.Request) {
    c, err := r.Cookie("session_id")
    if err == nil {
        sessionsMu.Lock()
        delete(sessions, c.Value)
        sessionsMu.Unlock()
        // expire the cookie
        c.Value = ""
        c.MaxAge = -1
        http.SetCookie(w, c)
    }
}

// ensureLoggedIn is a middleware that redirects to /login if the user
// has no valid session.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        if _, err := getSession(r); err != nil {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// settingsTmpl is our escaped HTML template for displaying the form.
var settingsTmpl = template.Must(template.New("settings").Parse(`
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Settings</title>
</head>
<body>
<h1>Hello, {{ .Username }}!</h1>
<form action="/settings" method="post">
  <input type="hidden" name="csrf_token" value="{{ .CSRFToken }}">
  <p>
    Email:
    <input type="email" name="email" value="{{ .Email }}">
  </p>
  <button type="submit">Update Settings</button>
</form>
<p><a href="/logout">Log out</a></p>
</body>
</html>
`))

// settingsHandler shows or updates the user’s settings.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Set some safe headers:
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'self'")

    sess, err := getSession(r)
    if err != nil {
        // no valid session; redirect to login
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    switch r.Method {
    case http.MethodGet:
        // Render the form.  In a real app you’d fetch the current email from your DB.
        data := struct {
            Username  string
            CSRFToken string
            Email     string
        }{
            Username:  sess.Username,
            CSRFToken: sess.CSRFToken,
            Email:     "", // e.g. fetch from DB
        }
        if err := settingsTmpl.Execute(w, data); err != nil {
            http.Error(w, "Server error", http.StatusInternalServerError)
        }

    case http.MethodPost:
        // 1) parse and validate form
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Invalid form", http.StatusBadRequest)
            return
        }
        // 2) check CSRF
        if r.FormValue("csrf_token") != sess.CSRFToken {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }
        // 3) grab & validate your new settings
        newEmail := strings.TrimSpace(r.FormValue("email"))
        if !isValidEmail(newEmail) {
            http.Error(w, "Invalid email address", http.StatusBadRequest)
            return
        }
        // 4) TODO: update your database with newEmail for sess.Username
        // For this example we just pretend it worked:

        // 5) Redirect back to GET so the user can see “fresh” values
        http.Redirect(w, r, "/settings", http.StatusSeeOther)

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

// A very naïve email checker; replace with a real regex / validator.
func isValidEmail(s string) bool {
    return len(s) > 3 && strings.Contains(s, "@")
}

// logoutHandler allows the user to end their session.
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    clearSession(w, r)
    http.Redirect(w, r, "/", http.StatusFound)
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Wrap settings & logout in our login-check middleware:
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/logout", ensureLoggedIn(logoutHandler))

    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}