package main

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"html/template"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
)

var sessionStore = sessions.NewFilesystemStore("sessions", []byte("super-secret-key"))

// Secure headers middleware
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline'; img-src 'self' https: data:; font-src 'self' https:")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("X-XSS-Protection", "1; mode=block")
		next(w, r)
	}
}

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", secureHeaders(settingsHandler))
	http.ListenAndServe(":8080", nil)
}

// Ensure logged in
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		session, err := sessionStore.Get(r, "secure_session")
		if err != nil {
			http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
			return
		}
		if session.Values["username"] == nil {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// Home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// Login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	csrfToken := generateCSRFToken()
	session, err := sessionStore.Get(r, "secure_session")
	if err != nil {
		http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
		return
	}
	session.Values["csrf_token"] = csrfToken
	session.Save(r.Context(), w)

	template := `<!DOCTYPE html>
<html>
<head><title>Login</title></head>
<body>
    <form action="/login_post" method="post">
        <p><input type="text" name="username" placeholder="Username" required></p>
        <p><input type="password" name="password" placeholder="Password" required></p>
        <input type="hidden" name="csrf_token" value="{{.}}">
        <button type="submit">Submit</button>
    </form>
    <style>body { font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; } input { display: block; margin-bottom: 10px; padding: 8px; width: 90%; }</style>
</body>
</html>`

	tmpl := template.New("login").Parse(template)
	if err := tmpl.Execute(w, csrfToken); err != nil {
		log.Printf("Error executing template: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
	}
}

func generateCSRFToken() string {
	b := make([]byte, 32)
	for i := range b {
		b[i] = byte(i)
	}
	hasher := sha256.New()
	hasher.Write(b)
	return hex.EncodeToString(hasher.Sum(nil))
}

// Login POST
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	session, err := sessionStore.Get(r, "secure_session")
	if err != nil {
		http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
		return
	}

	csrfToken := r.FormValue("csrf_token")
	if csrfToken != session.Values["csrf_token"] {
		fmt.Fprint(w, "Invalid CSRF token. Please refresh the page and try again.")
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "admin" && password == "password" {
		session.Values["username"] = username
		session.Options.MaxAge = 3600
		session.Save(r.Context(), w)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Invalid credentials. Please try again.")
	}
}

// Settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	template := `<!DOCTYPE html>
<html>
<head><title>Settings</title></head>
<body>
    <h1>Your Settings</h1>
    <p>Welcome back, {{.username}}!</p>
    <script>alert("This is a safe alert message!");</script>
    <style>.welcome { color: #333; font-weight: bold; }</style>
</body>
</html>`

	username := session["username"]
	if username == "" {
		http.Redirect(w, r, "/login", http.StatusFound)
		return
	}

	safeUsername := template.HTMLEscapeString(username)

	tmpl := template.New("settings").Parse(template)
	if err := tmpl.Execute(w, struct{ username string }{safeUsername}); err != nil {
		log.Printf("Error executing template: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
	}
}
