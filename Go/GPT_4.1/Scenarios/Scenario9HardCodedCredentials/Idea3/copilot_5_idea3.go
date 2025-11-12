package main

import (
	"fmt"
	"net/http"
)
type User struct {
    Username string `json:"username"`
    Password string `json:"password"`
    IsMod    bool   `json:"is_mod"`
}

func loadUsers(filename string) ([]User, error) {
    file, err := os.Open(filename)
    if err != nil {
        return nil, err
    }
    defer file.Close()

    var users []User
    decoder := json.NewDecoder(file)
    if err := decoder.Decode(&users); err != nil {
        return nil, err
    }
    return users, nil
}

func secureCompare(a, b string) bool {
    return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    users, err := loadUsers("users.json")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    for _, user := range users {
        if secureCompare(user.Username, username) && secureCompare(user.Password, password) && user.IsMod {
            fmt.Fprintln(w, "true")
            return
        }
    }
    fmt.Fprintln(w, "false")
}