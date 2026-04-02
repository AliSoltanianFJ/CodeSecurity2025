package main

import (
    "context"
    "database/sql"
    "encoding/base64"
    "encoding/json"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    "github.com/golang-jwt/jwt/v4"
    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    db        *sql.DB
    jwtSecret []byte
)

// Claims defines the structure of our JWT payload.
type Claims struct {
    Username string `json:"username"`
    jwt.RegisteredClaims
}

func init() {
    // 1) Load configuration from environment
    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbHost := os.Getenv("DB_HOST")
    dbName := os.Getenv("DB_NAME")
    jwtSecretB64 := os.Getenv("JWT_SECRET")
    if dbUser == "" || dbPass == "" || dbHost == "" || dbName == "" || jwtSecretB64 == "" {
        log.Fatal("One of DB_USER, DB_PASS, DB_HOST, DB_NAME or JWT_SECRET is not set")
    }

    // Decode JWT_SECRET if it's Base64 encoded
    var err error
    jwtSecret, err = base64.StdEncoding.DecodeString(jwtSecretB64)
    if err != nil {
        // If it wasn't valid base64, just take the raw string bytes
        jwtSecret = []byte(jwtSecretB64)
    }

    // 2) Open a single *sql.DB for the lifetime of the application.
    //    We enable parseTime and a UTF8MB4 collation by default.
    dsn := fmt.Sprintf(
        "%s:%s@tcp(%s)/%s?parseTime=true&collation=utf8mb4_unicode_ci",
        dbUser, dbPass, dbHost, dbName,
    )
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // Optional: tune your connection pool
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // 3) Verify we can actually talk to the database
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.PingContext: %v", err)
    }
}

func main() {
    http.HandleFunc("/login", loginHandler)
    log.Println("Starting server on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler logs in a user (POST only).
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // 1) Parse & validate input
    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // 2) Fetch the stored bcrypt hash for this username
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var storedHash string
    query := `SELECT password_hash FROM users WHERE username = ?`
    err := db.QueryRowContext(ctx, query, username).Scan(&storedHash)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // user not found
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        // some other DB error
        log.Printf("db error fetching user %q: %v", username, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 3) Compare the provided password to the stored bcrypt hash
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        // invalid password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // 4) Create a JWT that we will set in a cookie
    claims := &Claims{
        Username: username,
        RegisteredClaims: jwt.RegisteredClaims{
            ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
            IssuedAt:  jwt.NewNumericDate(time.Now()),
            NotBefore: jwt.NewNumericDate(time.Now()),
            Issuer:    "my.example.app",
            Subject:   username,
        },
    }
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
    tokenString, err := token.SignedString(jwtSecret)
    if err != nil {
        log.Printf("jwt.SignedString: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 5) Send it back in a Secure, HttpOnly cookie
    http.SetCookie(w, &http.Cookie{
        Name:     "auth_token",
        Value:    tokenString,
        Path:     "/",
        Expires:  time.Now().Add(24 * time.Hour),
        HttpOnly: true,
        Secure:   true, // set to false if you are not using TLS for testing
        SameSite: http.SameSiteLaxMode,
    })

    // 6) Optionally return a JSON response
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]string{
        "message": "login successful",
    })
}