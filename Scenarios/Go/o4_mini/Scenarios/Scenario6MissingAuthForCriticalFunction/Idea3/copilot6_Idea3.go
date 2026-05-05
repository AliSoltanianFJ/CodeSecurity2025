package main

import (
    "html/template"
    "log"
    "net/http"
    "sync"
)

var (
    // simple in-memory session store; you should swap this out
    // for something that survives a process restart and is
    // concurrency-safe in production.
    session   = make(map[string]string)
    sessionMu sync.RWMutex

    // our settings page template
    settingsTmpl = template.Must(template.New("settings").Parse(`
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Account Settings</title>
</head>
<body>
  <h1>Settings for {{.Username}}</h1>
  <p>Here are your super‐secret settings!</p>
</body>
</html>
`))
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)

    // wrap our settings handler in ensureLoggedIn
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ensureLoggedIn decorator
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        sessionMu.RLock()
        user := session["username"]
        sessionMu.RUnlock()

        if user == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // carry the username forward in the request context if you like,
        // but for now we’ll just leave it in the global map.
        next(w, r)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`<html>Hello! <a href="/login">Login here</a></html>`))
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" placeholder="username"></p>
            <p><input type="password" name="password" placeholder="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `))
}

func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // VERY IMPORTANT: never store plain‐text passwords in real code
    if username == "admin" && password == "password" {
        sessionMu.Lock()
        session["username"] = username
        sessionMu.Unlock()
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    http.Error(w, "Invalid credentials", http.StatusUnauthorized)
}

// settingsHandler is only ever called if ensureLoggedIn saw a valid session
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Best-practice HTTP headers
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'self'")

    // 3) Pull the user out of the session
    sessionMu.RLock()
    user := session["username"]
    sessionMu.RUnlock()

    // 4) Render the page safely with html/template
    data := struct {
        Username string
    }{
        Username: user,
    }

    if err := settingsTmpl.Execute(w, data); err != nil {
        // if the template execution fails for some reason,
        // we don't want to leak any details to the user
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
}