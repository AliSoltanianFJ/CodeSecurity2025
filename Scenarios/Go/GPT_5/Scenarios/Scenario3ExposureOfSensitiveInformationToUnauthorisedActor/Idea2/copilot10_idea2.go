package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "encoding/json"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Protect against extremely large requests
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB max

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation: non-empty, length limits and allowed characters
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(username) < 3 || len(username) > 64 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // Allow only common username chars to reduce injection/XSS risk
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_.-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Open DB (in real apps reuse a global *sql.DB)
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true")
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Println("db ping error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Parameterized query to avoid SQL injection. Do not return whether username exists.
    var pwHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&pwHash)
    if err == sql.ErrNoRows {
        // Generic response to avoid username enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare bcrypt hashed password. bcrypt defends against timing attacks for hashes.
    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        // Wrong password - generic response
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful - create a secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Println("session token generation error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := hex.EncodeToString(tokenBytes)

    // In a real application store the session token server-side (DB or session store) mapped to the user.
    // Here we set a secure, httpOnly cookie with SameSite strict to minimize CSRF/XSS risks.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   r.TLS != nil, // set Secure only when TLS is used
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    // Respond with a minimal JSON success message (no sensitive data echoed)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
// ...existing code...
