package main

import (
    "database/sql"
    "errors"
    "log"
    "net/http"
    "time"

    "github.com/gorilla/securecookie"
    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    // db is our application-wide connection pool.
    db *sql.DB

    // hashKey and blockKey should be random, long, and loaded from
    // your configuration/environment.  The hashKey is used to HMAC,
    // the blockKey to optionally encrypt.
    //
    // You can generate them with:
    //    securecookie.GenerateRandomKey(32) // hashKey
    //    securecookie.GenerateRandomKey(16) // blockKey
    cookieHashKey  = []byte("a-very-secret-hash-key-32-bytes!")
    cookieBlockKey = []byte("a-secret-16-byte-key")
    sCookie        = securecookie.New(cookieHashKey, cookieBlockKey)
)

// userRecord holds the columns we scan out of the DB.
type userRecord struct {
    Username     string
    PasswordHash []byte
}

func main() {
    var err error
    // Open doesn’t establish any connections immediately, it just prepares the pool.
    db, err = sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true")
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // Always ping to verify configuration is okay:
    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }
    // Optional tuning:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler logs the user in if the username/password are correct.
// On success it issues a signed (and encrypted) cookie named "session".
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST is allowed", http.StatusMethodNotAllowed)
        return
    }

    // ParseForm is called internally by FormValue, but you can call it explicitly
    // if you need to inspect r.PostForm or r.MultipartForm.
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic sanity check
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // 1) Lookup the user’s hashed password in the database.
    user, err := getUserByUsername(username)
    if err != nil {
        // Distinguish between “not found” and “actual DB error”
        if errors.Is(err, sql.ErrNoRows) {
            // Do not leak “user not found” vs “password incorrect” — 
            // treat both as “unauthorized” after a constant-time delay.
            bcrypt.CompareHashAndPassword([]byte("$2a$10$abcdefghijklmnopqrstuv"), []byte(password))
            http.Error(w, "invalid username or password", http.StatusUnauthorized)
            return
        }
        log.Printf("db error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 2) Compare the supplied password with the stored hash.
    if err := bcrypt.CompareHashAndPassword(user.PasswordHash, []byte(password)); err != nil {
        // wrong password
        http.Error(w, "invalid username or password", http.StatusUnauthorized)
        return
    }

    // 3) At this point the user is authenticated. Issue a session cookie.
    sessionData := map[string]string{
        "username": user.Username,
        // you can add more fields here (e.g. a session ID,
        // expiration timestamp, CSRF token, etc.)
    }
    encoded, err := sCookie.Encode("session", sessionData)
    if err != nil {
        log.Printf("securecookie.Encode: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    cookie := &http.Cookie{
        Name:     "session",
        Value:    encoded,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // set to false only if you are not using HTTPS (not recommended)
        SameSite: http.SameSiteStrictMode,
        // You can also set an explicit Expires or MaxAge
        Expires: time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("login successful"))
}

// getUserByUsername does the prepared-statement lookup.
func getUserByUsername(username string) (*userRecord, error) {
    stmt := `SELECT username, password_hash FROM users WHERE username = ? LIMIT 1`
    row := db.QueryRow(stmt, username)

    var u userRecord
    if err := row.Scan(&u.Username, &u.PasswordHash); err != nil {
        return nil, err
    }
    return &u, nil
}