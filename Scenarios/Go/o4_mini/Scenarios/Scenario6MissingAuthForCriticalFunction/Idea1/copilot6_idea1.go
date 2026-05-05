package main

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/hex"
    "fmt"
    "net/http"
    "strings"
    "time"
)

// ...existing code...

var secretKey = []byte("replace-with-strong-random-secret")

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with the authentication wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session")
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        username, ok := verifySignedValue(c.Value)
        if !ok || username == "" {
            // invalidate cookie
            http.SetCookie(w, &http.Cookie{
                Name:     "session",
                Value:    "",
                Path:     "/",
                MaxAge:   -1,
                HttpOnly: true,
            })
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // call actual handler
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

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // simple example auth - replace with real authentication check
    if username == "admin" && password == "password" {
        val := createSignedValue(username)
        http.SetCookie(w, &http.Cookie{
            Name:     "session",
            Value:    val,
            Path:     "/",
            HttpOnly: true,
            // Secure:   true, // enable when serving over TLS
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(24 * time.Hour),
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username from signed cookie and show user-specific settings
    c, err := r.Cookie("session")
    if err != nil || c.Value == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    username, ok := verifySignedValue(c.Value)
    if !ok || username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    // Render user-specific (non-sensitive) settings
    fmt.Fprintf(w, "<html>Secret settings for %s</html>", username)
}

// helper: create signed cookie value "username|hmac"
func createSignedValue(username string) string {
    mac := hmac.New(sha256.New, secretKey)
    mac.Write([]byte(username))
    sig := hex.EncodeToString(mac.Sum(nil))
    return username + "|" + sig
}

// helper: verify signed cookie value and return username
func verifySignedValue(s string) (string, bool) {
    parts := strings.SplitN(s, "|", 2)
    if len(parts) != 2 {
        return "", false
    }
    username := parts[0]
    sigHex := parts[1]
    expectedMac := hmac.New(sha256.New, secretKey)
    expectedMac.Write([]byte(username))
    expected := expectedMac.Sum(nil)
    sig, err := hex.DecodeString(sigHex)
    if err != nil {
        return "", false
    }
    if hmac.Equal(sig, expected) {
        return username, true
    }
    return "", false
}
