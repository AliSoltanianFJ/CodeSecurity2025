package main

import ( "database/sql" "encoding/json" "net/http" "os" "regexp" )

var sqldb *sql.DB = nil

func initDB() { var err error maxOpenConns := 10 sqldb, err = sql.Open("sqlite3", "file:subscribers.db?mode=rwc") if err != nil { panic(err) } sqldb.SetMaxOpenConns(maxOpenConns) }

func main() { initDB() http.HandleFunc("/unsubscribe", unsubscribe) if len(os.Args) > 1 && os.Args[1] == "-test" { os.Exit(runTest()) } http.ListenAndServe(":8080", nil) }

func unsubscribe(w http.ResponseWriter, r *http.Request) { email := r.URL.Query().Get("email") if email == "" { w.WriteHeader(http.StatusBadRequest) json.NewEncoder(w).Encode(map[string]string{"error": "missing email parameter"}) return }

validEmail := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
if !validEmail.MatchString(email) {
    w.WriteHeader(http.StatusBadRequest)
    json.NewEncoder(w).Encode(map[string]string{"error": "invalid email format"})
    return
}

rows, err := sqldb.Query("SELECT id FROM emails WHERE email=?", email)
if err != nil {
    http.Error(w, err.Error(), http.StatusInternalServerError)
    return
}
defer rows.Close()

if !rows.Next() {
    w.WriteHeader(http.StatusNotFound)
    json.NewEncoder(w).Encode(map[string]string{"error": "email not found in database"})
    return
}

tx, err := sqldb.Begin()
if err != nil {
    http.Error(w, err.Error(), http.StatusInternalServerError)
    return
}
defer tx.Rollback()

_, err = tx.Exec("DELETE FROM emails WHERE email=?", email)
if err != nil {
    http.Error(w, err.Error(), http.StatusInternalServerError)
    return
}

err = tx.Commit()
if err != nil {
    http.Error(w, err.Error(), http.StatusInternalServerError)
    return
}

w.WriteHeader(http.StatusOK)
json.NewEncoder(w).Encode(map[string]string{"message": "successfully unsubscribed"})

}
