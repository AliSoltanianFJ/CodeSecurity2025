package main

import (
	"database/sql"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
// loginHandler validates user credentials and sets a session cookie upon successful login.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Open DB connection with DSN from env
    dsn := os.Getenv("DSN")
    if dsn == "" {
        http.Error(w, "Missing DSN", http.StatusBadRequest)
        return
    }
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "DB connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare statement to prevent SQL injection
    stmt, err := db.Prepare("SELECT password FROM users WHERE username = ?")
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var hashedPassword string
    err = stmt.QueryRow(username).Scan(&hashedPassword)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "DB error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt
    err = bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
    if err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate session ID and set session cookie with HttpOnly and Secure flags
    sessionID := uuid.New().String()
    cookie := http.Cookie{
        Name:     "session",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        Secure:   os.Getenv("HTTPS_ONLY") == "true", // Only send over HTTPS
        MaxAge:   3600,                            // Session valid for 1 hour
    }
    http.SetCookie(w, &cookie)

    // Store session ID in DB for session management (optional)
    _, err = db.Exec("UPDATE users SET session_id = ? WHERE username = ?", sessionID, username)
    if err != nil {
        log.Printf("Error updating session: %v", err)
    }

    // Respond with success message
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}

func main() {
    http.HandleFunc("/login", loginHandler)
    log.Printf("Server listening on :8080")
    http.ListenAndServe(":8080", nil)
}
