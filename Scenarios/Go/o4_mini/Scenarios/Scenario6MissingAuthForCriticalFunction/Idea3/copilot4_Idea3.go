package main

import (
    "crypto/rand"
    "encoding/base64"
    "errors"
    "fmt"
    "html/template"
    "io"
    "log"
    "net/http"
    "regexp"
    "sync"
    "time"
)

// Session holds user data and a per‐session CSRF token
type Session struct {
    Username  string
    CSRFToken string
    // You can add more fields here (e.g. preferences, email, etc.)
}

// sessions is an in‐memory store of sessionID→Session
var (
    sessions   = make(map[string]*Session)
    sessionsMu sync.RWMutex
)

// tmplSettings is our settings page template
var tmplSettings = template.Must(template.New("settings").Parse(`
<html>
  <head><title>Settings</title></head>
  <body>
    <h1>Settings for {{.Username}}</h1>
    {{if .Updated}}
      <p style="color:green;">Your settings have been updated.</p>
    {{end}}
    <form method="POST" action="/settings">
      <!-- CSRF protection -->
      <input type="hidden" name="csrf_token" value="{{.CSRFToken}}">

      <p>
        <label for="display_name">Display Name</label><br>
        <input id="display_name" name="display_name" type="text" value="{{.DisplayName}}">
      </p>

      <p>
        <button type="submit">Save</button>
      </p>
    </form>

    <p><a href="/logout">Log out</a></p>
  </body>
</html>
`))

// settingsData is what we pass into the template
type settingsData struct {
    Username    string
    CSRFToken   string
    DisplayName string  // an example user setting
    Updated     bool    // did we just save?
}

// email / name validation regex (example)
var validName = regexp.MustCompile(`^[A-Za-z0-9 _-]{1,50}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/logout", ensureLoggedIn(logoutHandler))

    log.Println("Listening on :8080 …")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// indexHandler shows a public page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='/login'>Login here</a></html>`)
}

// loginHandler shows the login form (GET only)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
    <html>
      <form action="/login_post" method="post">
        <p><input type="text" name="username" placeholder="Username"></p>
        <p><input type="password" name="password" placeholder="Password"></p>
        <p><button type="submit">Submit</button></p>
      </form>
    </html>
    `)
}

// doLoginHandler processes login credentials (POST only)
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // TODO: replace this with a real user lookup + bcrypt check
    if username == "admin" && password == "password" {
        sid, err := newSession(username)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        setSessionCookie(w, sid)
        http.Redirect(w, r, "/settings", http.StatusSeeOther)
        return
    }

    http.Error(w, "Invalid credentials", http.StatusUnauthorized)
}

// settingsHandler handles both GET and POST to /settings
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    sess, err := getSession(r)
    if err != nil {
        // This should never happen: ensureLoggedIn already checked for you
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    switch r.Method {
    case http.MethodGet:
        // Render the form, pulling any existing user settings out of `sess`
        data := settingsData{
            Username:    sess.Username,
            CSRFToken:   sess.CSRFToken,
            DisplayName: "",     // look up the real display name in your DB
            Updated:     r.URL.Query().Get("updated") == "1",
        }
        tmplSettings.Execute(w, data)

    case http.MethodPost:
        // 1) Verify CSRF
        if r.FormValue("csrf_token") != sess.CSRFToken {
            http.Error(w, "Invalid CSRF token", http.StatusForbidden)
            return
        }

        dn := r.FormValue("display_name")

        // 2) Persist changes (e.g. into your database)
        //    For example: err := db.SaveDisplayName(sess.Username, dn)
        //    handle err…

        // 3) Redirect back – Post/Redirect/Get
        http.Redirect(w, r, "/settings?updated=1", http.StatusSeeOther)

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

// logoutHandler clears the session
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    sid, err := r.Cookie("session_id")
    if err == nil {
        sessionsMu.Lock()
        delete(sessions, sid.Value)
        sessionsMu.Unlock()
    }
    // clear cookie
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    "",
        Path:     "/",
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
    })
    http.Redirect(w, r, "/", http.StatusSeeOther)
}

/*––– session management helpers –––*/

// newSession creates a new session for a username
func newSession(username string) (string, error) {
    // 1) Generate a secure session ID
    sid, err := generateRandomString(32)
    if err != nil {
        return "", err
    }
    // 2) Generate a secure CSRF token
    csrf, err := generateRandomString(32)
    if err != nil {
        return "", err
    }
    // 3) Store it
    sessionsMu.Lock()
    sessions[sid] = &Session{
        Username:  username,
        CSRFToken: csrf,
    }
    sessionsMu.Unlock()
    return sid, nil
}

// getSession retrieves the session for this request (by cookie)
func getSession(r *http.Request) (*Session, error) {
    c, err := r.Cookie("session_id")
    if err != nil {
        return nil, err
    }
    sessionsMu.RLock()
    defer sessionsMu.RUnlock()
    sess, ok := sessions[c.Value]
    if !ok {
        return nil, errors.New("session not found")
    }
    return sess, nil
}

// setSessionCookie sends a secure cookie to the client
func setSessionCookie(w http.ResponseWriter, sid string) {
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    sid,
        Path:     "/",
        Expires:  time.Now().Add(24 * time.Hour),
        HttpOnly: true,
        Secure:   true,                 // only over HTTPS in production
        SameSite: http.SameSiteStrictMode,
    })
}

// ensureLoggedIn is your decorator to protect endpoints
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // make sure they have a valid session
        if _, err := getSession(r); err != nil {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// generateRandomString returns a URL‐safe, base64‐encoded securely generated random string.
func generateRandomString(n int) (string, error) {
    buf := make([]byte, n)
    if _, err := io.ReadFull(rand.Reader, buf); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(buf), nil
}