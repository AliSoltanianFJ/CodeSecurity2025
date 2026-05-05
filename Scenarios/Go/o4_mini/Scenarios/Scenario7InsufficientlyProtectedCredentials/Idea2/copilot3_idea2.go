// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/argon2"
    mysqlDriver "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Protect against very large requests
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20) // 10 MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Extract and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password") // do not trim; spaces may be intentional

    // Validate username: allow letters, digits, dot, underscore, hyphen; length limits
    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }
    validUserRegexp := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUserRegexp.MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }

    // Validate password: enforce minimum length and reasonable maximum
    if len(password) < 12 || len(password) > 1024 {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }

    // Hash the password using Argon2id with a random salt
    hash, err := generateArgon2idHash(password)
    if err != nil {
        // do not reveal internal error details to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Get DB DSN from environment to avoid embedding credentials in code
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Open DB and use context with timeout
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use prepared statement to avoid SQL injection
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hash)
    if err != nil {
        // Detect unique constraint duplicate username for MySQL driver
        if me, ok := err.(*mysqlDriver.MySQLError); ok {
            // 1062 = ER_DUP_ENTRY
            if me.Number == 1062 {
                http.Error(w, "User already exists", http.StatusConflict)
                return
            }
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Success: do not return sensitive details
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered")
}

// ...existing code...
// generateArgon2idHash returns an encoded hash using Argon2id with a random salt.
// The returned format is: $argon2id$v=19$m=<memory>,t=<time>,p=<threads>$<base64salt>$<base64hash>
func generateArgon2idHash(password string) (string, error) {
    // Parameters — chosen for reasonable security; adjust for your environment
    var (
        timeParam uint32 = 1
        memory    uint32 = 64 * 1024 // 64 MB
        threads   uint8  = 4
        keyLen    uint32 = 32
        saltLen          = 16
    )

    salt := make([]byte, saltLen)
    if _, err := rand.Read(salt); err != nil {
        return "", err
    }

    hash := argon2.IDKey([]byte(password), salt, timeParam, memory, threads, keyLen)

    b64Salt := base64.RawStdEncoding.EncodeToString(salt)
    b64Hash := base64.RawStdEncoding.EncodeToString(hash)

    encoded := fmt.Sprintf("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s", memory, timeParam, threads, b64Salt, b64Hash)
    return encoded, nil
}
// ...existing code...
