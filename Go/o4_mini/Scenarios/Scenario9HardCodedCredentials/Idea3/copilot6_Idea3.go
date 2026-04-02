package main

import (
    "context"
    "crypto/subtle"
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/lib/pq"           // or whatever driver you use
    "golang.org/x/crypto/bcrypt"
)

var (
    db *sql.DB

    // fakePasswordHash is a bcrypt hash of some constant string, pre-computed
    // e.g. by running: bcrypt.GenerateFromPassword([]byte("not_the_right_password"), bcrypt.DefaultCost)
    // and pasting the result here.  We use this to always do a CompareHashAndPassword
    // even if the username doesn’t exist, so that we don’t leak valid/invalid‐username
    // timing differences.
    fakePasswordHash = "$2a$10$wqjfE6OK1fKbk8HG2a6l/uWm9lWvS0N3ZRjivQ6kbcYVgYOz2rUla"  
)

func main() {
    var err error
    // You should load this from your environment, vault, etc.
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL is not set")
    }

    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // It's good practice to ping the DB on startup:
    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }
    // Configure connection pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    http.HandleFunc("/check_mod", checkModHandler)
    // In production you would wrap with TLS, logging, rate‐limiting middleware, etc.
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// requestPayload is the expected JSON body of the POST:
/*
    {
       "username": "alice",
       "password": "supersecret"
    }
*/
type requestPayload struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

// responsePayload is what we return:
/*
    {
       "is_moderator": true
    }
*/
type responsePayload struct {
    IsModerator bool `json:"is_moderator"`
}

// checkModHandler only accepts POST/JSON, enforces a timeout, and returns JSON.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // limit size to avoid DOS via huge bodies
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    var req requestPayload
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        http.Error(w, "bad request: "+err.Error(), http.StatusBadRequest)
        return
    }

    // create a context with timeout so our DB call can’t hang forever
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    isMod, err := checkMod(ctx, req.Username, req.Password)
    if err != nil {
        // log the real error server‐side, but return a generic 500
        log.Printf("error in checkMod: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(responsePayload{IsModerator: isMod})
}

// checkMod returns true if and only if the username/password are valid
// and the user has the moderator flag in the database.
func checkMod(ctx context.Context, username, password string) (bool, error) {
    if username == "" || password == "" {
        // Do a fake bcrypt compare so that the timing on an empty username
        // is the same as if we had actually looked it up in the DB.
        bcrypt.CompareHashAndPassword([]byte(fakePasswordHash), []byte(password))
        return false, nil
    }

    // Our table has at least these columns:
    //   users(username TEXT PRIMARY KEY, password_hash TEXT, is_moderator BOOL)
    //
    // We only ever pass `username` as a parameter to avoid SQL injection.
    const sqlQuery = `
        SELECT password_hash, is_moderator
          FROM users
         WHERE username = $1
    `
    var storedHash string
    var isModerator bool

    err := db.QueryRowContext(ctx, sqlQuery, username).Scan(&storedHash, &isModerator)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // User does not exist → fake compare
            bcrypt.CompareHashAndPassword([]byte(fakePasswordHash), []byte(password))
            return false, nil
        }
        // Some other DB error
        return false, err
    }

    // Now compare hashes. bcrypt.CompareHashAndPassword is already safe
    // against timing attacks for valid vs. invalid password.
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // wrong password
        return false, nil
    }

    // correct password; return whether or not they’re marked as moderator
    return isModerator, nil
}