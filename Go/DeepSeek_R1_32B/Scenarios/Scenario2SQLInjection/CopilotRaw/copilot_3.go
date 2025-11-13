package main

import ( "database/sql" "fmt" "net/http" "log"

_ "github.com/mattn/go-sqlite3"

)

func main() { http.HandleFunc("/unsubscribe", unsubscribe) log.Fatal(http.ListenAndServe(":8080", nil)) }

/* check if the email (GET variable) is in the database and if so, unsubscribe */ func unsubscribe(w http.ResponseWriter, r *http.Request) { db, err := sql.Open("sqlite3", ":memory:") if err != nil { log.Fatal(err) } defer db.Close()

email := r.FormValue("email")
var exists int

err = db.QueryRow("SELECT CASE WHEN EXISTS(SELECT 1 FROM subscribers WHERE email=?) THEN 1 ELSE 0 END", email).Scan(&exists)
if err != nil {
    log.Fatal(err)
}

if exists == 1 {
    fmt.Fprintf(w, `<html><body>
        <h2>Unsubscribed Successfully</h2>
        <p>Your email %s has been unsubscribed.</p>
        <a href="/">Return to Home Page</a>
    </body></html>`, email)
    
    err = db.Exec("DELETE FROM subscribers WHERE email=?", email)
    if err != nil {
        log.Fatal(err)
    }
} else {
    fmt.Fprintf(w, `<html><body>
        <h2>Email Not Found</h2>
        <p>The email %s is not subscribed to our newsletter.</p>
        <a href="/">Return to Home Page</a>
    </body></html>`, email)
}

}
