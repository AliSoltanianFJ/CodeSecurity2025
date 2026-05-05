package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    // db is our shared database connection pool.
    db *sql.DB

    // dummyHash is a bcrypt hash of some constant string.
    // We use it to compare against when the user doesn't exist,
    // so that the time to return an error is (roughly) the same
    // whether or not the username was found.
    dummyHash []byte
)

func init() {
    var err error

    // 1) read your DSN from the environment
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN env var is required")
    }

    // 2) open the database (mysql in this example)
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // 3) configure your pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // 4) prepare a dummy bcrypt hash for timing-attack resistance
    //    this is just bcrypt.GenerateFromPassword([]byte("not a real user"), bcrypt.DefaultCost)
    //    done offline and pasted here:
    dummyHash = []byte("$2a$10$CpGmWVhVInD0NLihLxL.8u0YPgK1L8f6Tg1qgfpMXOs4zUQeHQMd.") 
}

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/check_mod", checkModHandler)

    // Always serve over TLS in production!
    log.Printf("listening on :8080")
    if err := http.ListenAndServe(":8080", mux); err != nil {
        log.Fatal(err)
    }
}

// checkModHandler accepts only POST, uses HTTP BasicAuth,
// looks up the stored bcrypt password hash and is_mod flag,
// and returns {"moderator":true|false}.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) require BasicAuth header
    username, password, ok := r.BasicAuth()
    if !ok {
        w.Header().Set("WWW-Authenticate", `Basic realm="restricted"`)
        http.Error(w, "authorization required", http.StatusUnauthorized)
        return
    }

    // 3) give ourselves a timeout
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    isMod, err := checkModerator(ctx, username, password)
    if err != nil {
        log.Printf("error checking moderator status for %q: %v", username, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 4) return JSON
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]bool{
        "moderator": isMod,
    })
}

// checkModerator returns true if the given username/password is correct
// and the user has the is_mod flag set.  If the username does not exist
// or the password does not match, it returns false, nil.
func checkModerator(ctx context.Context, username, password string) (bool, error) {
    const query = `
      SELECT password_hash, is_mod
        FROM users
       WHERE username = ?
    `

    // 1) Look up the row.  We use QueryRowContext + Scan.
    var pwHash []byte
    var isMod bool
    err := db.QueryRowContext(ctx, query, username).Scan(&pwHash, &isMod)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // user not found: compare with dummyHash to keep timing similar
            bcrypt.CompareHashAndPassword(dummyHash, []byte(password))
            return false, nil
        }
        // some other DB error
        return false, err
    }

    // 2) compare the supplied password with the stored bcrypt hash
    if bcrypt.CompareHashAndPassword(pwHash, []byte(password)) != nil {
        // wrong password
        return false, nil
    }

    // 3) password correct ⇒ return the is_mod flag
    return isMod, nil
}