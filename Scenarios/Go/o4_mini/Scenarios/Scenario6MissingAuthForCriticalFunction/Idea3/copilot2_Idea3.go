package main

import (
    "crypto/rand"
    "crypto/subtle"
    "encoding/hex"
    "fmt"
    "html/template"
    "net/http"
    "time"

    "golang.org/x/crypto/bcrypt"
)

const (
    sessionCookieName = "session_id"
    csrfFormField     = "csrf_token"
)

// Session holds data for a logged-in user
type Session struct {
    Username  string
    CSRFToken string
}

// In-memory stores (for demo purposes)
// In production you’d back these by a database, not plain maps.
var (
    sessions      = map[string]Session{}
    userPasswords = map[string][]byte{}   // username → bcrypt hash
)

func init() {
    // initialize our single “admin” user
    hash, _ := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    userPasswords["admin"] = hash
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // wrap settingsHandler so it automatically checks for a valid session cookie
    http.HandleFunc("/settings", settingsHandler)
    http.ListenAndServe(":8080", nil)
}

// index just points you to the login page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href="/login">Login here</a></html>`)
}

// loginHandler shows the login form
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html>
        <form action="/login_post" method="post">
            <p><input type="text" name="username" placeholder="username"></p>
            <p><input type="password" name="password" placeholder="password"></p>
            <p><button type="submit">Submit</button></p>
        </form>
        </html>
    `)
}

// doLoginHandler validates credentials and, on success, sets a session cookie
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // lookup the stored hash
    hashed, ok := userPasswords[username]
    if !ok || bcrypt.CompareHashAndPassword(hashed, []byte(password)) != nil {
        // bad login
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // 1) generate a brand-new session ID
    sessionID := mustGenerateRandomToken(32)
    // 2) generate a CSRF token
    csrfTok := mustGenerateRandomToken(32)

    // 3) store in our in-memory session store
    sessions[sessionID] = Session{
        Username:  username,
        CSRFToken: csrfTok,
    }

    // 4) set a cookie
    http.SetCookie(w, &http.Cookie{
        Name:     sessionCookieName,
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true if you’re on HTTPS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    })

    http.Redirect(w, r, "/settings", http.StatusSeeOther)
}

// settingsTemplate is our HTML form for GET
var settingsTemplate = template.Must(template.New("settings").Parse(`
<html>
  <body>
    <h1>Settings for {{ .Username }}</h1>
    <form method="POST" action="/settings">
      <input type="hidden" name="{{ .CSRFField }}" value="{{ .CSRFToken }}">
      <p>
        Change Password (leave blank to keep current):
        <input type="password" name="new_password">
      </p>
      <p>
        <button type="submit">Save</button>
      </p>
    </form>
  </body>
</html>
`))

// settingsHandler serves GET and POST /settings
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Fetch session cookie
    c, err := r.Cookie(sessionCookieName)
    if err != nil {
        http.Redirect(w, r, "/login", http.StatusSeeOther)
        return
    }

    sess, ok := sessions[c.Value]
    if !ok {
        // invalid or expired session
        http.Redirect(w, r, "/login", http.StatusSeeOther)
        return
    }

    switch r.Method {
    case http.MethodGet:
        // render the form, injecting the CSRF token
        settingsTemplate.Execute(w, map[string]string{
            "Username":   sess.Username,
            "CSRFField":  csrfFormField,
            "CSRFToken":  sess.CSRFToken,
        })

    case http.MethodPost:
        // parse form
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Bad request", http.StatusBadRequest)
            return
        }
        // 2) CSRF check (constant time)
        posted := r.PostFormValue(csrfFormField)
        if subtle.ConstantTimeCompare([]byte(posted), []byte(sess.CSRFToken)) != 1 {
            http.Error(w, "Invalid CSRF token", http.StatusForbidden)
            return
        }

        // 3) If they supplied a new password, enforce policy + rehash
        newPass := r.PostFormValue("new_password")
        if newPass != "" {
            if len(newPass) < 8 {
                http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
                return
            }
            hashed, err := bcrypt.GenerateFromPassword([]byte(newPass), bcrypt.DefaultCost)
            if err != nil {
                http.Error(w, "Server error", http.StatusInternalServerError)
                return
            }
            userPasswords[sess.Username] = hashed
        }

        // (Optionally: regenerate a fresh CSRF token for the next form)
        newCsrf := mustGenerateRandomToken(32)
        sess.CSRFToken = newCsrf
        sessions[c.Value] = sess

        // Finally redirect back to GET
        http.Redirect(w, r, "/settings", http.StatusSeeOther)

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

// mustGenerateRandomToken returns a hex-encoded secure random string of length 2*n.
// It panics if rand.Read fails (extremely unlikely).
func mustGenerateRandomToken(n int) string {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        panic("unable to read random bytes: " + err.Error())
    }
    return hex.EncodeToString(b)
}