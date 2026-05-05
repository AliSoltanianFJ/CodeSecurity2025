// ...existing code...
package main

import (
    "database/sql"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_]{3,30}$`)

func main() {
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("missing MYSQL_DSN environment variable")
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("database open error")
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        log.Fatal("database ping error")
    }

    http.HandleFunc("/register", registerHandler(db))
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
func registerHandler(db *sql.DB) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        if r.Method != http.MethodPost {
            http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
            return
        }

        if err := r.ParseForm(); err != nil {
            http.Error(w, "invalid form", http.StatusBadRequest)
            return
        }

        username := strings.TrimSpace(r.FormValue("username"))
        password := r.FormValue("password")

        if username == "" || password == "" {
            http.Error(w, "username and password required", http.StatusBadRequest)
            return
        }

        if !usernameRegex.MatchString(username) {
            http.Error(w, "invalid username", http.StatusBadRequest)
            return
        }

        if len(password) < 12 || len(password) > 128 {
            http.Error(w, "invalid password", http.StatusBadRequest)
            return
        }

        hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
        if err != nil {
            log.Printf("password hash error: %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }

        _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashed))
        if err != nil {
            if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
                http.Error(w, "username already taken", http.StatusConflict)
                return
            }
            log.Printf("db insert error: %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }

        w.WriteHeader(http.StatusCreated)
    }
}
// ...existing code...
