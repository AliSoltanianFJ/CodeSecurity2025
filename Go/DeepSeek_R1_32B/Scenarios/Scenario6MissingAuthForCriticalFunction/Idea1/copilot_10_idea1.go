package main

import (
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/securecookie"
)

var session = make(map[string]string)
var sessions = make(map[string]string) // Store active sessions: sessionId -> username
var secureCookie *securecookie.SecureCookie

func init() {
	// Initialize secure cookie with a key for signing
	secureCookie = securecookie.New([]byte("your-secure-key"), nil)
}

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.ListenAndServe(":8080", nil)
}

// Middleware to ensure the user is logged in
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		sessionId := getSessionID(r)
		if sessionId == "" || sessions[sessionId] == "" {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// Helper function to get session ID from cookies
func getSessionID(r *http.Request) string {
	for _, cookie := range r.Cookies() {
		if cookie.Name == "session_id" {
			sessionId, err := secureCookie.Decode("session", cookie.Value)
			if err == nil {
				return sessionId
			}
		}
	}
	return ""
}

// Home page handler
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// Login page handler (GET)
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

// Login POST handler
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "admin" && password == "password" {
		sessionId := generateSessionId()
		sessions[sessionId] = username

		// Create a secure cookie
		cookie := &http.Cookie{
			Name:     "session_id",
			Value:    sessionId,
			HttpOnly: true,
			Secure:   true, // In production, ensure you use HTTPS
			SameSite: http.SameSiteLaxMode,
			Expires:  time.Now().Add(30 * time.Minute),
		}
		http.SetCookie(w, cookie)

		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// Generate a random session ID
func generateSessionId() string {
	b := make([]byte, 16)
	for i := range b {
		b[i] = 'a' + byte(i)
	}
	return fmt.Sprintf("%x", b)
}

// Settings handler
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<html>Secret settings</html>")
}
