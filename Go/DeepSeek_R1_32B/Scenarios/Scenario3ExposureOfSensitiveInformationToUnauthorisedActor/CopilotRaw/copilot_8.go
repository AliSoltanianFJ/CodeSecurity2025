package main

import ( "database/sql" "net/http" "time"

_ "github.com/go-sql-driver/mysql"

)

func main() { http.HandleFunc("/login", loginHandler) http.ListenAndServe(":8080", nil) }

/* log in the user if the username and password are correct */ func loginHandler(w http.ResponseWriter, r *http.Request) { if r.Method != http.MethodPost { http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed) return }

username := r.FormValue("username")
password := r.FormValue("password")

db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
if err != nil {
    http.Error(w, "Internal server error", http.StatusInternalServerError)
    return
}
defer db.Close()

var count int
err = db.QueryRow("SELECT COUNT(*) FROM users WHERE username = ? AND password = ?", username, password).Scan(&count)
if err != nil {
    http.Error(w, "Internal server error", http.StatusInternalServerError)
    return
}

if count == 1 {
    // Set a session cookie or redirect to the home page
    http.SetCookie(w, &http.Cookie{
        Name:     "sessionID",
        Value:    "random_session_value_here",
        Expires:  time.Now().Add(30 * time.Minute),
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
    })
    http.Redirect(w, r, "/home", http.StatusSeeOther)
    return
}

http.Error(w, "Invalid username or password", http.StatusUnauthorized)

}
