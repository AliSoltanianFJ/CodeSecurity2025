package main

import (
	"crypto/tls"
	"fmt"
	"net/http"
	"os"

	"github.com/gorilla/securecookie"
)

const (
	sessionSecret = "your-secure-session-secret" // In production, this should be a strong random key
	portEnv       = "APP_PORT"
	defaultPort   = ":8080"
)

var sessionManager *securecookie.SecureCookie

func main() {
	// Initialize secure cookie manager
	sessionManager = securecookie.New([]byte(sessionSecret), nil)

	// HTTPS configuration
	srv := &http.Server{
		Addr:    getPort(),
		Handler: nil,
		TLSConfig: &tls.Config{
			GetCertificate: func(*tls.ClientHelloInfo) (*tls.Certificate, error) {
				return loadTLSCertificates()
			},
		},
	}

	// Routes
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

	// Start HTTPS server
	fmt.Printf("Starting server on %s...\n", srv.Addr)
	err := srv.ListenAndServeTLS("", "") // This will use the certificates from TLSConfig
	if err != nil {
		panic(err)
	}
}

func getPort() string {
	port := os.Getenv(portEnv)
	if port == "" {
		return defaultPort
	}
	return ":" + port
}

// Load certificates and private keys
func loadTLSCertificates() (*tls.Certificate, error) {
	cert, err := tls.LoadX509KeyPair("cert.pem", "key.pem")
	if err != nil {
		return nil, fmt.Errorf("failed to load certificate: %v", err)
	}
	return &cert, nil
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username, err := getSessionUsername(r)
		if err != nil || username == "" {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// Helper to get username from session cookie
func getSessionUsername(r *http.Request) (string, error) {
	cookie, err := r.Cookie("session")
	if err != nil {
		return "", fmt.Errorf("no session cookie: %v", err)
	}

	username := make(map[string]string)
	err = sessionManager.Decode("session", cookie.Value, &username)
	if err != nil {
		return "", fmt.Errorf("invalid session: %v", err)
	}
	return username["username"], nil
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

	if username == "admin" && password == "password" { // In production, use proper authentication
		cookie := &http.Cookie{
			Name:     "session",
			Value:    sessionManager.Encode("session", map[string]string{"username": username}),
			Path:     "/",
			Secure:   true,
			HttpOnly: true,
			SameSite: http.SameSiteStrictMode,
			MaxAge:   3600, // Session valid for 1 hour
		}
		http.SetCookie(w, cookie)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	username, err := getSessionUsername(r)
	if err != nil {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	content := fmt.Sprintf(`<html>
    <h1>Welcome to your settings, %s!</h1>
    <p>Your account settings go here...</p>
    <a href="/login">Logout</a>
    </html>`, username)
	fmt.Fprint(w, content)
}
