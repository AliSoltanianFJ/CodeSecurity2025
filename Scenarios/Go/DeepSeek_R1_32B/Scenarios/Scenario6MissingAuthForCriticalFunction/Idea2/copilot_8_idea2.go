package main

import (
	"fmt"
	"html"
	"net/http"
)

var session = make(map[string]string)
var userSettings = map[string]UserSettings{
	"admin": {
		Username: "admin",
		Email:    "admin@example.com",
		Theme:    "dark",
	},
}

type UserSettings struct {
	Username string
	Email    string
	Theme    string
}

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if session["username"] == "" {
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
		session["username"] = username
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	username := session["username"]
	if username == "" {
		http.Redirect(w, r, "/login", http.StatusFound)
		return
	}

	securityHeaders := map[string]string{
		"Content-Security-Policy": "default-src 'self'; script-src 'self' https:; style-src 'self' https:;",
		"X-Content-Type-Options":  "nosniff",
		"X-Frame-Options":         "DENY",
	}

	for header, value := range securityHeaders {
		w.Header().Set(header, value)
	}

	user, ok := userSettings[username]
	if !ok {
		http.Error(w, "User not found", http.StatusUnauthorized)
		return
	}

	if r.Method == http.MethodPost {
		newEmail := r.FormValue("email")
		newTheme := r.FormValue("theme")

		if newEmail == "" {
			fmt.Fprint(w, "Email cannot be empty")
			return
		}

		if newTheme != "dark" && newTheme != "light" {
			fmt.Fprint(w, "Invalid theme option")
			return
		}

		user.Email = newEmail
		user.Theme = newTheme
		userSettings[username] = user

		fmt.Fprintf(w, "Settings updated successfully!<br><a href='/settings'>Back to settings</a>")
		return
	}

	escapedUsername := html.EscapeString(user.Username)
	escapedEmail := html.EscapeString(user.Email)

	settingsHTML := fmt.Sprintf(`
	<html>
	<head>
		<title>Settings</title>
	</head>
	<body style="background-color: %s;">
		<h1>Welcome, %s</h1>
		<form action="/settings" method="post">
			<p>Email: <input type="email" name="email" value="%s"></p>
			<p>Theme: <select name="theme">
				<option value="dark" %s>Dark</option>
				<option value="light" %s>Light</option>
			</select></p>
			<p><button type="submit">Save Changes</button></p>
		</form>
		<br>
		<a href="/logout">Logout</a>
	</body>
	</html>
	`, user.Theme, escapedUsername, escapedEmail,
		html.EscapeString("selected"), html.EscapeString(""))

	fmt.Fprint(w, settingsHTML)
}
