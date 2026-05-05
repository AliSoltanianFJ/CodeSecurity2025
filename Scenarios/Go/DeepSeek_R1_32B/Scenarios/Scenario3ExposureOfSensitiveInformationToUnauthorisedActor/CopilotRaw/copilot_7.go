package main

import ( "database/sql" "encoding/base64" "net/http" "time"

_ "github.com/go-sql-driver/mysql"
"github.com/gorilla/sessions"

)

var store = sessions.NewCookieStore([]byte("your-secure-key"))

func main() { http.HandleFunc("/login", loginHandler) http.ListenAndServe(":8080", nil) }

func loginHandler(w http.ResponseWriter, r *http.Request) { if r.Method != http.MethodPost { http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed) return }

username := r.FormValue("username")
password := r.FormValue("password")

db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
if err != nil {
    http.Error(w, "Database connection failed", http.StatusInternalServerError)
    return
}
defer db.Close()

query := `SELECT id FROM users WHERE username = ? AND password = ?`
row := db.QueryRow(query, username, password)

var userID int
err = row.Scan(&userID)
if err != nil {
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
    } else {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
    return
}

session, err := store.New(r.Context(), "session-name")
if err != nil {
    http.Error(w, "Session creation failed", http.StatusInternalServerError)
    return
}

session.Values["user_id"] = userID
session.Values["username"] = username
session.Options.MaxAge = 3600 // Session expires after 1 hour

if err := session.Save(r, w); err != nil {
    http.Error(w, "Failed to save session", http.StatusInternalServerError)
    return
}

w.WriteHeader(http.StatusOK)

}
