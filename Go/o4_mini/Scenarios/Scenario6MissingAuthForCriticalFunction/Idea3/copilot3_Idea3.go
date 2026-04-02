package main

import (
    "crypto/rand"
    "encoding/base64"
    "html/template"
    "io"
    "log"
    "net/http"
    "sync"
    "time"
)

const (
    // how long sessions remain valid
    sessionTTL       = 30 * time.Minute
    sessionCookieKey = "session_id"
)

// a little in-memory “user database” of settings
// in real life you would persist this in a real database
var userSettings = struct {
    sync.RWMutex
    m map[string]string
}{m: make(map[string]string)}

// our server-side session store
type sessionData struct {
    Username  string
    CSRFToken string
    Expires   time.Time
}

var (
    sessions   = make(map[string]*sessionData)
    sessionsMu sync.RWMutex
)

// generateRandomString returns a URL-safe, base64-encoded random string.
func generateRandomString(n int) (string, error) {
    b := make([]byte, n)
    if _, err := io.ReadFull(rand.Reader, b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// newSession should be called when a user successfully logs in.
func newSession(w http.ResponseWriter, username string) (*sessionData, string, error) {
    /* ... implement session creation logic ... */
}

// getSession looks up the sessionData for this request, if any.
// Returns (nil, "") if there is no valid session.
func getSession(r *http.Request) (*sessionData, string) {
    c, err := r.Cookie(sessionCookieKey)
    if err != nil {
        return nil, ""
    }
    sessionsMu.RLock()
    sd, ok := sessions[c.Value]
    sessionsMu.RUnlock()
    if !ok {
        return nil, ""
    }
    if time.Now().After(sd.Expires) {
        // expired
        sessionsMu.Lock()
        delete(sessions, c.Value)
        sessionsMu.Unlock()
        return nil, ""
    }
    return sd, c.Value
}

// ensureLoggedIn is a decorator that rejects any request if the session
// is not present or expired.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        sd, _ := getSession(r)
        if sd == nil {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // you could attach sd to the context here if you like
        next(w, r)
    }
}

var settingsTmpl = template.Must(template.New("settings").Parse(`
<!DOCTYPE html>
<html>
  <head><meta charset="utf-8"><title>Your Settings</title></head>
  <body>
    <h1>Settings for {{.Username}}</h1>
    {{if .Success}}
      <p style="color:green">Settings updated successfully.</p>
    {{end}}
    <form method="POST" action="/settings">
      <input type="hidden" name="csrf_token" value="{{.CSRFToken}}">
      <p>
        <label>Your favorite color:
          <input type="text" name="favorite_color" value="{{.FavoriteColor}}">
        </label>
      </p>
      <button type="submit">Save</button>
    </form>
    <p><a href="/">Home</a></p>
  </body>
</html>
`))

// settingsHandler serves GET to render the form and
// POST to update the settings.  It is protected by ensureLoggedIn.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    sd, sessID := getSession(r)
    if sd == nil {
        // just in case someone calls it directly
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    switch r.Method {
    case http.MethodGet:
        // look up existing value
        userSettings.RLock()
        color := userSettings.m[sd.Username]
        userSettings.RUnlock()

        data := struct {
            Username      string
            CSRFToken     string
            FavoriteColor string
            Success       bool
        }{
            Username:      sd.Username,
            CSRFToken:     sd.CSRFToken,
            FavoriteColor: color,
            Success:       false,
        }
        settingsTmpl.Execute(w, data)

    case http.MethodPost:
        // parse the form, check CSRF
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Bad request", http.StatusBadRequest)
            return
        }
        formToken := r.PostForm.Get("csrf_token")
        if formToken == "" || formToken != sd.CSRFToken {
            http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
            return
        }

        // take the new setting
        newColor := r.PostForm.Get("favorite_color")

        // you can do any server-side validation here
        // store it
        userSettings.Lock()
        userSettings.m[sd.Username] = newColor
        userSettings.Unlock()

        // regenerate CSRF token so that a stolen form doesn't stay valid
        newCSRF, err := generateRandomString(32)
        if err != nil {
            log.Println("unable to regenerate CSRF token:", err)
        } else {
            sessionsMu.Lock()
            sessions[sessID].CSRFToken = newCSRF
            sessionsMu.Unlock()
            sd.CSRFToken = newCSRF
        }

        // re-render the page with a “success” message
        data := struct {
            Username      string
            CSRFToken     string
            FavoriteColor string
            Success       bool
        }{
            Username:      sd.Username,
            CSRFToken:     sd.CSRFToken,
            FavoriteColor: newColor,
            Success:       true,
        }
        settingsTmpl.Execute(w, data)

    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Wrap settingsHandler in ensureLoggedIn
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ----------------------------------
// everything below here is unchanged
// ----------------------------------

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    io.WriteString(w, `<html>Hello! <a href="/login">Login here</a></html>`)
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    io.WriteString(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // BASIC check -- replace with real lookup in a secure user store
    if username == "admin" && password == "password" {
        _, _, err := newSession(w, username)
        if err != nil {
            log.Println("error creating session:", err)
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        io.WriteString(w, "Bad username or password")
    }
}