package main

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
)

var sessionStore = sessions.NewCookieStore([]byte("your-secure-key-here"))
var currentSession map[string]string = make(map[string]string)

func main() {
	http.HandleFunc("/", indexHandler)
	loginmux := http.NewServeMux()
	loginmux.HandleFunc("/login", loginHandler)
	loginmux.HandleFunc("/login_post", doLoginHandler)
	http.Handle("/login/", ensureLoggedIn(loginmux))
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

	// Secure server configuration
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		session, err := sessionStore.Get(r, "session_id")
		if err != nil || session.Values["username"] == nil {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
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
	csrfToken := generateCSRFToken()
	session, _ := sessionStore.New(r, "csrf_token")
	session.Values["csrf_token"] = csrfToken
	session.Save(r, w)

	fmt.Fprintf(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username"></p>
			<p><input type="password" name="password"></p>
			<input type="hidden" name="csrf_token" value="%s">
			<p><button type="submit">Submit</button></p>
		</form></html>
	`, csrfToken)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	username := r.FormValue("username")
	password := r.FormValue("password")
	csrfToken := r.FormValue("csrf_token")

	session, err := sessionStore.Get(r, "csrf_token")
	if err != nil || session.Values["csrf_token"] == nil {
		http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
		return
	}

	defer session.Destroy()
	if csrfToken != session.Values["csrf_token"].(string) {
		http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
		return
	}

	if username == "admin" && password == "password" { // In real application use secure hashing like bcrypt
		session.ID = generateSessionID()
		currentSession[session.ID] = username
		session.Save(r, w)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<html>Secret settings</html>")
}

func generateCSRFToken() string {
	b := make([]byte, 32)
	rand.Read(b)
	return base64.URLEncoding.EncodeToString(b)
}

func generateSessionID() string {
	b := make([]byte, 32)
	rand.Read(b)
	return base64.URLEncoding.EncodeToString(b)
}
