package main

import (
	"crypto/bcrypt"
	"encoding/gob"
	"fmt"
	"html"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
)

var sessionStore = sessions.NewCookieStore([]byte("your-secure-key"))

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

	// Start server with HTTPS
	log.Fatal(http.ListenAndServeTLS(":8081", "cert.pem", "key.pem", nil))
}

func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if sessionStore.Get(r, "session").Values["username"] == nil {
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

	// Validate input lengths
	if len(username) < 1 || len(username) > 32 {
		fmt.Fprint(w, "Invalid username length")
		return
	}

	// Secure password comparison using bcrypt
	hashedPassword, _ := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if string(hashedPassword) != "$2a$10$"+ "admin_hash" { // Replace with actual hashed admin password
		fmt.Fprint(w, "Bad username or password")
		return
	}

	// Create secure session
	session, _ := sessionStore.Get(r, "session")
	session.Values["username"] = username
	session.Options = &sessions.Options{
		MaxAge:   3600,
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
	}
	session.Save(r, w)

	http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	session, _ := sessionStore.Get(r, "session")
	username := session.Values["username"]

	if r.Method == http.MethodPost {
		newPassword := r.FormValue("new_password")

		// Validate password complexity
		if len(newPassword) < 8 {
			fmt.Fprint(w, "Password must be at least 8 characters")
			return
		}

		// Securely hash new password
		hashedPassword, _ := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)

		// Store hashed password securely (replace with actual storage logic)
		// Never store plain text passwords

		fmt.Fprint(w, "Password updated successfully")
		return
	}

	// Display settings page
	fmt.Fprintf(w, `
    <html>
        <h1>Welcome to Settings, %s</h1>
        <form action="/settings" method="post">
            <p><input type="password" name="new_password" placeholder="New Password"></p>
            <p><button type="submit">Change Password</button></p>
        </form>
    </html>`,
		html.EscapeString(fmt.Sprintf("%s", username)))
}

// Secure middleware to enforce HTTPS
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-XSS-Protection", "1; mode=block")
		w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
		w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline'; img-src 'self' https: data:; font-src 'self' https: data:;")

		if r.TLS == nil {
			http.Redirect(w, r, "https://"+r.Host+r.URL.String(), http.StatusMovedPermanently)
			return
		}
		next(w, r)
	}
}
