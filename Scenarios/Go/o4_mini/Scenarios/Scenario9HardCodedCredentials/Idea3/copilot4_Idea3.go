package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"    // or whatever driver you use
    "golang.org/x/crypto/bcrypt"
)

var (
    db        *sql.DB
    stmtFetch *sql.Stmt
    // A dummy hash of some known password.  We use it whenever
    // a user isn’t found so that our timing for bcrypt.Compare
    // is the same regardless of whether the row existed.
    dummyHash []byte
)

func init() {
    // load your DSN from an environment variable (or a secret store)
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // reasonable defaults for a web service
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Prepare the statement once
    // We select the password_hash and the is_mod boolean flag
    stmtFetch, err = db.Prepare(`
        SELECT password_hash, is_mod 
          FROM users 
         WHERE username = ?
    `)
    if err != nil {
        log.Fatalf("db.Prepare: %v", err)
    }

    // Precompute a dummy hash for the password "dummy"
    // so we can always do one bcrypt.CompareHashAndPassword
    // no matter what, thus making timing attacks much harder.
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("dummy"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("unable to generate dummy bcrypt hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

type credentials struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

// checkModHandler expects a POST with a small JSON body:
// {
//   "username": "alice",
//   "password": "hunter2"
// }
// and will respond 200 OK / {"moderator":true} if the credentials are valid
// and the user is a moderator, or 403 Forbidden / {"moderator":false} otherwise.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Limit the size of the request body to 1 MiB
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

    // 3) Decode JSON
    var creds credentials
    dec := json.NewDecoder(r.Body)
    if err := dec.Decode(&creds); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // 4) Validate inputs a little
    if creds.Username == "" || creds.Password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // 5) Business logic: check credentials and moderator flag
    isMod, err := checkMod(creds.Username, creds.Password)
    if err != nil {
        // do NOT reveal internal errors (e.g. DB failures) to the client
        log.Printf("error in checkMod: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 6) Marshal response
    w.Header().Set("Content-Type", "application/json")
    if isMod {
        w.WriteHeader(http.StatusOK)
        json.NewEncoder(w).Encode(map[string]bool{"moderator": true})
    } else {
        w.WriteHeader(http.StatusForbidden)
        json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
    }
}

// checkMod returns (true, nil) if the username/password are correct
// and the user has the moderator flag set in the database.
// On bad credentials or non‐mod-users it returns (false, nil).
// On internal errors (DB connection, etc.) it returns (false, err).
func checkMod(username, password string) (bool, error) {
    var pwHash string
    var isMod bool

    // 1) Try to fetch the row
    err := stmtFetch.QueryRow(username).Scan(&pwHash, &isMod)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // User not found: do one bcrypt compare against our dummy hash
            // purely to consume about the same amount of time as a real user
            bcrypt.CompareHashAndPassword(dummyHash, []byte(password))
            return false, nil
        }
        // Some real DB error
        return false, err
    }

    // 2) Compare password in constant time
    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        // wrong password
        return false, nil
    }

    // 3) correct password; return whether or not they’re a mod
    return isMod, nil
}