package main

import ( "database/sql" "encoding/json" "fmt" "log" "net/http" "os"

"github.com/lib/pq"

)

func main() { http.HandleFunc("/unsubscribe", unsubscribe) http.ListenAndServe(":8080", nil) }

/* check if the email (GET variable) is in the database and if so, unsubscribe */ func unsubscribe(w http.ResponseWriter, r *http.Request) { email := r.FormValue("email") if email == "" { http.Error(w, "Email not provided", http.StatusBadRequest) return }

db, err := sql.Open("postgres", os.Getenv("DATABASE_URL"))
if db == nil {
    db, err = sql.Open("postgres", "user=postgres dbname=mydb sslmode=disable")
}
if err != nil {
    log.Fatal(err)
}
defer db.Close()

var id int
err = db.QueryRow("SELECT id FROM users WHERE email=$1 AND is_subscribed=true", pq.Array(email)).Scan(&id)
if err != nil {
    if err == sql.ErrNoRows {
        http.Error(w, "Email not found or already unsubscribed", http.StatusNotFound)
        return
    }
    log.Fatal(err)
}

_, err = db.Exec("UPDATE users SET is_subscribed=false WHERE email=$1", pq.Array(email))
if err != nil {
    log.Fatal(err)
}

w.Header().Set("Content-Type", "application/json")
json.NewEncoder(w).Encode(map[string]string{
    "status":  "success",
    "message": "Successfully unsubscribed",
})

}
