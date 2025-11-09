package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...

var fakeHash []byte

func init() {
    // Prepare a fake bcrypt hash to use when a username is not found.
    // This prevents timing differences between "user not found" and "bad password".
    var err error
    fakeHash, err = bcrypt.GenerateFromPassword([]byte("invalid-password-placeholder"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to generate fake bcrypt hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form data safely
    if err := r.ParseForm(); err != nil {
        // Do not reveal parsing details to the client
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Get and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password") // do not trim password; preserve whitespace if user intended

    // Basic validation: length limits and allowed characters for username
    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    // restrict username characters to a safe subset to reduce injection/vector surface
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Limit password length to prevent large payload abuse
    if len(password) == 0 || len(password) > 1024 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment so credentials are not checked into source code.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Log internal detail only on the server; return a generic error to client.
        log.Println("missing DB_DSN environment variable")
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Open DB and ensure connection is healthy with context and timeout
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Query for the stored password hash using a prepared statement (parameterized query).
    var storedHash []byte
    userExists := true
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err != nil {
        if err == sql.ErrNoRows {
            // User not found: use fake hash to make timing similar to a real user
            storedHash = fakeHash
            userExists = false
        } else {
            // Other DB error: log and return generic error
            log.Printf("db query error: %v", err)
            http.Error(w, "Server error", http.StatusInternalServerError)
            return
        }
    }

    // Compare password using bcrypt. This is constant-time for the hash algorithm.
    compareErr := bcrypt.CompareHashAndPassword(storedHash, []byte(password))
    if compareErr != nil || !userExists {
        // Generic message for any auth failure (prevents username enumeration)
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Authentication successful: create a secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    sessionToken := base64.RawURLEncoding.EncodeToString(tokenBytes)

    // Set cookie: HttpOnly and SameSite; set Secure only if connection is TLS.
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(24 * time.Hour),
        Secure:   r.TLS != nil,
    }
    http.SetCookie(w, cookie)

    // NOTE: sessionToken should be associated server-side (e.g., in DB or in-memory store)
    // with the authenticated user. That storage is omitted here for brevity and must be implemented.

    // Respond with minimal, escaped message to avoid XSS and leakage of sensitive info.
    _, _ = w.Write([]byte(html.EscapeString("Login successful")))
}

// ...existing code...
