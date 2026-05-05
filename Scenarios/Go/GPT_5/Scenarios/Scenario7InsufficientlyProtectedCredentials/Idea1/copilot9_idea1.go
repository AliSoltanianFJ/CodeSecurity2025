// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // DSN should come from environment in real deployments. Example fallback kept minimal for local testing.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "user:password@tcp(localhost:3306)/testdb?parseTime=true"
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    // verify connection with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // parse form (handles urlencoded and multipart)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }
    // basic password policy (adjust as needed)
    if len(password) < 8 {
        http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // bcrypt hashing with a safe cost. bcrypt stores salt internally.
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Error processing password", http.StatusInternalServerError)
        return
    }

    // insert user using context with timeout and parameterized query to avoid SQL injection
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // It's good practice to ensure a unique index on username in DB; handle duplicate errors gracefully.
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashedPassword))
    if err != nil {
        // attempt to detect duplicate key (driver-specific). Keep response generic to avoid leaking info.
        http.Error(w, fmt.Sprintf("Error registering user: %v", err), http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}
