package main

import (
	"crypto/md5"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"time"

	"github.com/google/uuid"
)

var session = make(map[string]string)
var sessions map[string]*UserSession
var CSRFTokens = make(map[string]string)

type UserSession struct {
	username  string
	token     string
	expiresAt time.Time
}

func main() {
	rand.Seed(time.Now().UnixNano())
	sessions = make(map[string]*UserSession)
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.HandleFunc("/change_password", passwordChangeHandler)
	http.HandleFunc("/logout", logoutHandler)
	http.ListenAndServe(":8080", nil)
}

func generateToken() string {
	b := make([]byte, 32)
	rand.Read(b)
	return fmt.Sprintf("%x", md5.Sum(b))
}

func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if session["username"] == "" {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

func parseCookie(r *http.Request) string {
	for _, cookie := range r.Cookies() {
		if cookie.Name == "session_token" {
			return cookie.Value
		}
	}
	return ""
}

func setSessionCookie(w http.ResponseWriter, token string) {
	cookie := &http.Cookie{
		Name:     "session_token",
		Value:    token,
		Path:     "/",
		Expires:  time.Now().Add(30 * time.Minute),
		HttpOnly: true,
		Secure:   false, // Set to true in production if using HTTPS
		SameSite: http.SameSiteLaxMode,
	}
	http.SetCookie(w, cookie)
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

	if username == "admin" && password == "password" {
		token := generateToken()
		sessionID := uuid.New().String()
		sessions[sessionID] = &UserSession{
			username:  username,
			token:     token,
			expiresAt: time.Now().Add(30 * time.Minute),
		}
		setSessionCookie(w, sessionID)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	sessionID := parseCookie(r)
	if session, ok := sessions[sessionID]; ok && !session.expiresAt.Before(time.Now()) {
		csrfToken := generateToken()
		CSRFTokens[r.FormValue("username")] = csrfToken
		fmt.Fprintf(w, `
			<html>
				<h1>Welcome %s</h1>
				<a href="/logout">Logout</a><br>
				<form action="/change_password" method="post">
					<input type="password" name="current_password" placeholder="Current Password">
					<br>
					<input type="password" name="new_password" placeholder="New Password">
					<br>
					<input type="hidden" name="csrf_token" value="%s">
					<br>
					<button type="submit">Change Password</button>
				</form>
			</html>
		`, session.username, csrfToken)
	} else {
		http.Redirect(w, r, "/login", http.StatusFound)
	}
}

func passwordChangeHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	sessionID := parseCookie(r)
	if session, ok := sessions[sessionID]; ok && !session.expiresAt.Before(time.Now()) {
		username := session.username
		currentPassword := r.FormValue("current_password")
		newPassword := r.FormValue("new_password")
		csrfToken := r.FormValue("csrf_token")

		if CSRFTokens[username] != csrfToken {
			http.Error(w, "Invalid CSRF Token", http.StatusBadRequest)
			return
		}

		if currentPassword == "password" && newPassword != "" {
			// Update password logic here
			fmt.Fprint(w, "Password changed successfully")
			return
		}
		fmt.Fprint(w, "Invalid current password or empty new password")
	} else {
		http.Redirect(w, r, "/login", http.StatusFound)
	}
}

func logoutHandler(w http.ResponseWriter, r *http.Request) {
	sessionID := parseCookie(r)
	delete(sessions, sessionID)
	http.SetCookie(w, &http.Cookie{
		Name:     "session_token",
		Value:    "",
		Path:     "/",
		Expires:  time.Unix(0, 0),
		HttpOnly: true,
	})
	http.Redirect(w, r, "/", http.StatusFound)
}
