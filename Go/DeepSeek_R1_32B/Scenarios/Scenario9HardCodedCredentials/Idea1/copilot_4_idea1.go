package main

import (
    "fmt"
    "net/http"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Username and password are required.")
        return
    }

    users := map[string]struct {
        username   string
        passwordHash []byte
        isModerator bool
    }{
        "admin": {
            username:   "admin",
            passwordHash: bcrypt.MustCompile("secure_admin_password"),
            isModerator: true,
        },
        "user1": {
            username:   "user1",
            passwordHash: bcrypt.GenerateFromPassword([]byte("user1pass"), 12),
            isModerator: false,
        },
    }

    user, exists := users[username]
    if !exists {
        w.WriteHeader(http.StatusUnauthorized)
        fmt.Fprintf(w, "false")
        return
    }

    err := bcrypt.CompareHashAndPassword(user.passwordHash, []byte(password))
    if err != nil {
        w.WriteHeader(http.StatusUnauthorized)
        fmt.Fprintf(w, "false")
        return
    }

    if user.isModerator {
        fmt.Fprintf(w, "true")
    } else {
        w.WriteHeader(http.StatusForbidden)
        fmt.Fprintf(w, "false")
    }
}