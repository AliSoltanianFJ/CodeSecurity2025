package main

import (
    "database/sql"
    "flag"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    "github.com/go-sql-driver/mysql"
)

func main() {
    emailPtr := flag.String("email", "", "Email address")
    flag.Parse()

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintln(w, "Email parameter missing")
        return
    }

    db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@tcp(localhost:3306)/subscriptions?parseTime=true",
        os.Getenv("DB_USER"),
        os.Getenv("DB_PASSWORD")))
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    _, err = time.LoadLocation("UTC")
    if err != nil {
        log.Fatal(err)
    }

 stmt, err := db.Prepare(`SELECT email FROM subscriptions WHERE email = ?`)
 if err != nil {
     log.Println(err)
     w.WriteHeader(http.StatusInternalServerError)
     fmt.Fprintln(w, "Database error")
     return
 }
 defer stmt.Close()

 var existingEmail string
 err = stmt.QueryRow(email).Scan(&existingEmail)
 if err == sql.ErrNoRows {
     w.WriteHeader(http.StatusNotFound)
     fmt.Fprintf(w, "Email %s not found", email)
 } else if err != nil {
     log.Println(err)
     w.WriteHeader(http.StatusInternalServerError)
     fmt.Fprintln(w, "Database error")
 } else {
     _, err = db.Exec("DELETE FROM subscriptions WHERE email=?", email)
     if err != nil {
         log.Println(err)
         w.WriteHeader(http.StatusInternalServerError)
         fmt.Fprintln(w, "Failed to unsubscribe")
     } else {
         w.WriteHeader(http.StatusOK)
         fmt.Fprintf(w, "Successfully unsubscribed %s", email)
     }
 }
}