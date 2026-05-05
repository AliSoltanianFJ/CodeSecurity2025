package main


import (
 "database/sql"
 "fmt"
 "log"
 "net/http"
 "net/mail"
 "strings"


 _ "github.com/go-sql-driver/mysql"
)


func main() {
 http.HandleFunc("/unsubscribe", unsubscribeHandler)
 log.Fatal(http.ListenAndServe(":8080", nil))
}


func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
 db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
 if err != nil {
  log.Println("Failed to connect to DB:", err)
  http.Error(w, "DB connection error", http.StatusInternalServerError)
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


 email = sanitizeEmail(email)


 stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
 if err != nil {
  log.Println("Failed to prepare statement:", err)
  http.Error(w, "DB query error", http.StatusInternalServerError)
  return
 }
 defer stmt.Close()


 result, err := stmt.Exec(email)
 if err != nil {
  log.Println("Failed to execute query:", err)
  http.Error(w, "DB execution error", http.StatusInternalServerError)
  return
 }


 rowsAffected, err := result.RowsAffected()
 if err != nil {
  log.Println("Failed to get affected rows:", err)
  http.Error(w, "Error getting affected rows", http.StatusInternalServerError)
  return
 }


 if rowsAffected > 0 {
  fmt.Fprintln(w, "Unsubscribed successfully!")
 } else {
  fmt.Fprintln(w, "Email not found or already unsubscribed.")
 }
}


func isValidEmail(email string) bool {
 addr, err := mail.ParseAddress(email)
 if err != nil {
  return false
 }
 if !strings.Contains(addr.Address, "@") {
  return false
 }
 return true
}


func sanitizeEmail(email string) string {
 email = strings.TrimSpace(email)
 return email
}