package main


import (
 "database/sql"
 "fmt"
 "log"
 "net/http"
 "net/url"
 "strings"


 _ "github.com/go-sql-driver/mysql"
)


func main() {
 http.HandleFunc("/unsubscribe", unsubscribe)
 log.Fatal(http.ListenAndServe(":8080", nil))
}


func unsubscribe(w http.ResponseWriter, r *http.Request) {
 db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
 if err != nil {
  http.Error(w, "DB connection failed", http.StatusInternalServerError)
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
  http.Error(w, "Invalid email", http.StatusBadRequest)
  return
 }


 sanitizedEmail := sanitize(email)


 stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
 if err != nil {
  http.Error(w, "Prepare failed", http.StatusInternalServerError)
  log.Println(err)
  return
 }
 defer stmt.Close()


 res, err := stmt.Exec(sanitizedEmail)
 if err != nil {
  http.Error(w, "Exec failed", http.StatusInternalServerError)
  log.Println(err)
  return
 }


 rows, err := res.RowsAffected()
 if err != nil {
  http.Error(w, "RowsAffected failed", http.StatusInternalServerError)
  log.Println(err)
  return
 }


 if rows > 0 {
  fmt.Fprintln(w, "Unsubscribed!")
 } else {
  fmt.Fprintln(w, "Not found or already unsubscribed.")
 }
}


func isValidEmail(email string) bool {
 _, err := url.ParseRequestURI("mailto:" + email)
 if err != nil {
  return false
 }
 if !strings.Contains(email, "@") {
  return false
 }
 return true
}


func sanitize(s string) string {
 return s
}