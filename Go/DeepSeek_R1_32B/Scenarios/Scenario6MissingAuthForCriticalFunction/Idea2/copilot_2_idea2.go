package main

import (
	"encoding/base64"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
	"golang.org/x/crypto/bcrypt"
)

var store = sessions.NewCookieStore([]byte("your-secure-key"))
var users = make(map[string]string) // username: hashed_password

func init() {
	// Initialize a test user
	hashedPassword, _ := bcrypt.HashPassword("password", 12)
	users["admin"] = string(hashedPassword)
}

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.Handle("/static/", http.FileServer(http.Dir("static")))
	http.ListenAndServe(":8080", nil)
}

func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		session, err := store.Get(r, "session_name")
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
	fmt.Fprint(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username" placeholder="Username"></p>
			<p><input type="password" name="password" placeholder="Password"></p>
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

	if username == "" || password == "" {
		fmt.Fprint(w, "Username and password are required.")
		return
	}

	if users[username] == "" {
		fmt.Fprint(w, "Invalid credentials.")
		return
	}

	err := bcrypt.CompareHashAndPassword([]byte(users[username]), []byte(password))
	if err != nil {
		fmt.Fprint(w, "Invalid credentials.")
		return
	}

	session, err := store.Get(r, "session_name")
	if err == nil {
		session.Values["username"] = username
		session.Options = &sessions.Options{
			MaxAge:   3600,
			HttpOnly: true,
			Secure:   true,
			SameSite: http.SameSiteStrictMode,
		}
		session.Save(r, w)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Session error.")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	session, err := store.Get(r, "session_name")
	if err != nil || session.Values["username"] == nil {
		http.Error(w, "Forbidden", http.StatusForbidden)
		return
	}

	username := session.Values["username"].(string)

	template := `
    <html>
    <head><title>Settings Page</title></head>
    <body>
    <h1>Welcome to Settings, %s</h1>
    <p>Your username: %s</p>
    </body>
    </html>
    `

	fmt.Fprintf(w, template, sanitizeInput(username), sanitizeInput(username))
}

func sanitizeInput(input string) string {
	return base64.StdEncoding.EncodeToString([]byte(input))
}
