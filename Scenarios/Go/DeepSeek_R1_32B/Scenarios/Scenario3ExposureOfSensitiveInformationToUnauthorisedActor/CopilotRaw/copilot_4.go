package main

import ( "database/sql" "net/http"

_ "github.com/go-sql-driver/mysql"

)

func main() { http.HandleFunc("/login", loginHandler) http.ListenAndServe(":8080", nil) }

func loginHandler(w http.ResponseWriter, r *http.Request) { if r.Method != http.MethodPost { http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed) return }

username := r.FormValue("username")
password := r.FormValue("password")

db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
if err != nil {
    http.Error(w, "Internal server error", http.StatusInternalServerError)
    return
}
defer db.Close()

var foundUsername string
err = db.QueryRow("SELECT username FROM users WHERE username = ? AND password = ?", username, password).Scan(&foundUsername)
if err == sql.ErrNoRows {
    http.Error(w, "Invalid credentials", http.StatusUnauthorized)
    return
} else if err != nil {
    http.Error(w, "Internal server error", http.StatusInternalServerError)
    return
}

// Set session or cookie here
w.Write([]byte("OK"))

}
