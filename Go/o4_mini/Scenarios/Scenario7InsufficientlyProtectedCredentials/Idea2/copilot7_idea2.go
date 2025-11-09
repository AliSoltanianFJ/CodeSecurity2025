package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "fmt"
    "html"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"
    "unicode/utf8"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/argon2"
)

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request body to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Collect and normalise inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate username: length and allowed characters
    if l := utf8.RuneCountInString(username); l < 3 || l > 64 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }
    // Allow a conservative set of username characters
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Validate password: enforce minimum length and reasonable maximum
    if l := utf8.RuneCountInString(password); l < 12 || l > 1024 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Do not log or echo passwords. Use Argon2id with a random salt.
    const (
        saltLen      = 16
        argTime      = 1
        argMemoryKB  = 64 * 1024 // 64 MB
        argParallel  = 4
        argKeyLength = 32
    )

    salt := make([]byte, saltLen)
    if _, err := rand.Read(salt); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    hash := argon2.IDKey([]byte(password), salt, argTime, argMemoryKB, argParallel, argKeyLength)

    // Encode values for storage
    encHash := base64.StdEncoding.EncodeToString(hash)
    encSalt := base64.StdEncoding.EncodeToString(salt)

    // Obtain DSN from environment (do not hardcode credentials)
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Store in database using parameterized queries and a short context timeout
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    // Ensure DB is reachable
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Use parameterized INSERT to avoid SQL injection
    _, err = db.ExecContext(ctx,
        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
        username, encHash, encSalt,
    )
    if err != nil {
        // Don't reveal internal details or whether username already exists
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    // Return minimal non-sensitive confirmation. Escape static output for safety.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, html.EscapeString("Account created"))
}
