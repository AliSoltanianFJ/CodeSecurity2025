package main

import (
    "fmt"
    "net/http"
)

// ...existing code...
var session = make(map[string]string)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // ensure /settings is protected
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
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

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    user := session["username"]
    fmt.Fprintf(w, "<html><h1>Settings for %s</h1><p>Secret settings</p></html>", user)
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario6MissingAuthForCriticalFunction\Scenario6.go
package main

import (
    "fmt"
    "net/http"
)

// ...existing code...
var session = make(map[string]string)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // ensure /settings is protected
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
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

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    user := session["username"]
    fmt.Fprintf(w, "<html><h1>Settings for %s</h1><p>Secret settings</p></html>", user)
}
