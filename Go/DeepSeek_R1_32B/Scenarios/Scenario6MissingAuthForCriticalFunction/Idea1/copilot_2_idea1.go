package main

import (
	"fmt"
	"net/http"

	"github.com/gorilla/sessions"
)

var store = sessions.NewCookieStore([]byte("your-secure-key-here")) // Replace with a 32-byte key

func main() {
	store.Options = &sessions.Options{
		Path:     "/",
		Domain:   "localhost", // Update for production
		HttpOnly: true,
		Secure:   false,       // Set to true in production
	}

	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		session, err := store.Get(r, "session")
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		username := session.Values["username"]
		if username == nil {
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

	if username == "admin" && password == "password" {
		session, err := store.Get(r, "session")
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		session.Values["username"] = username
		err = session.Save(r, w)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	session, err := store.Get(r, "session")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	username := session.Values["username"]
	fmt.Fprintf(w, `<html>Secret settings for %s</html>`, username)
}
