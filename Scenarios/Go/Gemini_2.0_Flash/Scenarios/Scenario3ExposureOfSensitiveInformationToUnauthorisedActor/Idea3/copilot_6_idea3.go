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
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "DB connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var dbPassword string
    if err := db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&dbPassword); err == sql.ErrNoRows {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "DB query failed", http.StatusInternalServerError)
        return
    }

    // Use bcrypt for password comparison
    if err := bcrypt.CompareHashAndPassword([]byte(dbPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Set user ID in a secure cookie
    uid, err := getUserID(username, db)
    if err != nil {
        http.Error(w, "Failed to get user ID", http.StatusInternalServerError)
        return
    }
    http.SetCookie(w, &http.Cookie{
        Name:     "user_id",
        Value:    strconv.Itoa(uid),
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
    })

    // Log the successful login
    log.Printf("User '%s' logged in", username)
    w.Write([]byte("Login successful"))
}

// getUserID fetches the user ID from the database
func getUserID(username string, db *sql.DB) (int, error) {
    var uid int
    err := db.QueryRow("SELECT id FROM users WHERE username = ?", username).Scan(&uid)
    return uid, err
}
