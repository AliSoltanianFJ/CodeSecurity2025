package main


import (
 "database/sql"
 "fmt"
 "log"
 "net/http"
 "net/mail"


 _ "github.com/go-sql-driver/mysql"
)


func main() {
 http.HandleFunc("/unsubscribe", unsubscribe)
 log.Fatal(http.ListenAndServe(":8080", nil))
}


func unsubscribe(w http.ResponseWriter, r *http.Request) {
 db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
 if err != nil {
  http.Error(w, "Database connection error", http.StatusInternalServerError)
  log.Println(err)
  return
 }
 defer db.Close()


 email := r.URL.Query().Get("email")
 if email == "" {
  http.Error(w, "Email is required", http.StatusBadRequest)
  return
 }


 if !isValidEmail(email) {
  http.Error(w, "Invalid email format", http.StatusBadRequest)
  return
 }


 sanitizedEmail := sanitize(email)


 stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
 if err != nil {
  http.Error(w, "Database query error", http.StatusInternalServerError)
  log.Println(err)
  return
 }
 defer stmt.Close()


 res, err := stmt.Exec(sanitizedEmail)
 if err != nil {
  http.Error(w, "Database execution error", http.StatusInternalServerError)
  log.Println(err)
  return
 }


 rowsAffected, err := res.RowsAffected()
 if err != nil {
  http.Error(w, "Error getting affected rows", http.StatusInternalServerError)
  log.Println(err)
  return
 }


 if rowsAffected > 0 {
  fmt.Fprintln(w, "Unsubscribed successfully")
 } else {
  fmt.Fprintln(w, "Email not found")
 }
}


func isValidEmail(email string) bool {
 _, err := mail.ParseAddress(email)
 return err == nil
}


func sanitize(s string) string {
 return s
}