package main

import (
    "context"
    "crypto/rand"
    "crypto/subtle"
    "encoding/hex"
    "html/template"
    "log"
    "net/http"
    "sync"
    "time"
)

// Session holds all of our per‐user, per‐session data.
type Session struct {
    Username  string
    CSRFToken string
    // You could add more per-user settings here, e.g.:
    // Email     string
}

// sessionsMap stores active sessions by sessionID.
// We guard it with a mutex so that concurrent requests are safe.
var (
    sessions   = make(map[string]*Session)
    sessionsMu sync.RWMutex
)

// key type for context
type contextKey string

const sessionContextKey = contextKey("session")

func main() {
    // public endpoints
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)

    // protected endpoints
    // wrap settingsHandler in ensureLoggedIn
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// indexHandler is a public landing page.
func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`<html>Hello! <a href="/login">Login here</a></html>`))
}

// loginHandler displays a simple login form.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`
        <html><body>
        <form action="/login_post" method="post">
          <p>Username: <input type="text" name="username"></p>
          <p>Password: <input type="password" name="password"></p>
          <button type="submit">Login</button>
        </form>
        </body></html>
    `))
}

// doLoginHandler processes the login form.
// In a real app you would look up the user’s salted+hashed password in a database.
// Here we just accept admin/password for demo.
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // TODO: replace with your real user‐lookup + bcrypt/check
    if username != "admin" || password != "password" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // create a new session
    sid, err := generateRandomToken(32)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // initialize the session struct
    sess := &Session{
        Username: username,
        // CSRFToken gets generated on GET /settings
    }

    // store it in our map
    sessionsMu.Lock()
    sessions[sid] = sess
    sessionsMu.Unlock()

    // set the cookie
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    sid,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // if you are serving over HTTPS
        SameSite: http.SameSiteStrictMode,
        // optionally set a reasonable MaxAge or Expires
        Expires: time.Now().Add(24 * time.Hour),
    })

    // redirect into the protected area
    http.Redirect(w, r, "/settings", http.StatusFound)
}

// ensureLoggedIn is a middleware that looks up the user’s session_id cookie,
// validates it, injects the session into the request context, or else redirects
// to /login.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_id")
        if err != nil {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        sid := c.Value

        sessionsMu.RLock()
        sess, ok := sessions[sid]
        sessionsMu.RUnlock()
        if !ok {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // inject into context and call the next handler
        ctx := context.WithValue(r.Context(), sessionContextKey, sess)
        next(w, r.WithContext(ctx))
    }
}

// settingsTemplate is a small html/template for the settings page.
// The built-in escaping will protect you from reflected XSS.
var settingsTemplate = template.Must(template.New("settings").Parse(`
<!DOCTYPE html>
<html>
  <head><meta charset="utf-8"></head>
  <body>
    <h1>Settings for {{ .Username | html }}</h1>
    <form method="POST" action="/settings">
      <input type="hidden" name="csrf_token" value="{{ .CSRFToken | html }}">
      <p>New email: <input type="email" name="email" value="{{ .Email | html }}"></p>
      <button type="submit">Save</button>
    </form>
  </body>
</html>
`))

// settingsHandler serves GET and POST for /settings.
// It reads the session from the context (injected by ensureLoggedIn),
// on GET it generates a fresh CSRF token, stores it in the session, and
// renders the form. On POST it validates the token, does simple email
// validation, updates the session, and redirects back.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // pull our session struct out of the context
    sessAny := r.Context().Value(sessionContextKey)
    if sessAny == nil {
        // this really should never happen if ensureLoggedIn is wired up correctly
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }
    sess := sessAny.(*Session)

    switch r.Method {
    case http.MethodGet:
        // generate a new CSRF token for this form
        token, err := generateRandomToken(32)
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }

        // store it in the session
        sessionsMu.Lock()
        sess.CSRFToken = token
        sessionsMu.Unlock()

        // render the form
        data := struct {
            Username  string
            CSRFToken string
            Email     string
        }{
            Username:  sess.Username,
            CSRFToken: token,
            Email:     "", // if you had stored an email you could pre-fill it here
        }

        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        if err := settingsTemplate.Execute(w, data); err != nil {
            log.Println("template exec:", err)
            http.Error(w, "Internal error", http.StatusInternalServerError)
        }

    case http.MethodPost:
        // our form is small; ParseForm is fine
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Bad request", http.StatusBadRequest)
            return
        }

        submittedToken := r.FormValue("csrf_token")

        // grab the token we stored
        sessionsMu.RLock()
        expected := sess.CSRFToken
        sessionsMu.RUnlock()

        // constant-time compare to avoid timing attacks
        if subtle.ConstantTimeCompare([]byte(submittedToken), []byte(expected)) != 1 {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }

        // example of validating the email field
        newEmail := r.FormValue("email")
        if !isValidEmail(newEmail) {
            http.Error(w, "Invalid email address", http.StatusBadRequest)
            return
        }

        // “apply” the change — in this example we just store it in the session,
        // but in a real app you’d update your user database here.
        sessionsMu.Lock()
        // e.g. sess.Email = newEmail
        sessionsMu.Unlock()

        // Always redirect after state-changing POST to avoid double-POSTs
        http.Redirect(w, r, "/settings", http.StatusSeeOther)

    default:
        // tell the client what we allow
        w.Header().Set("Allow", "GET, POST")
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

// generateRandomToken returns a hex‐encoded securely random string of n bytes.
func generateRandomToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// isValidEmail is a very simplistic check.  In real life you might use
// a regexp or a third‐party library, or simply send a confirmation link.
func isValidEmail(email string) bool {
    // “foo@bar” is the bare minimum
    at := len(email) - len(“@”)
    return len(email) > 3 && at > 0 && email[at] == '@'
}