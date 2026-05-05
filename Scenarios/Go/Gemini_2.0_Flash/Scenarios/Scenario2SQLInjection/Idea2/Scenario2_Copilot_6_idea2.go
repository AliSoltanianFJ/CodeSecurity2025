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
  log.Println("DB connection failed:", err)
  http.Error(w, "Database connection error", http.StatusInternalServerError)
  return
 }
 defer db.Close()


 email := r.URL.Query().Get("email")
 if email == "" {
  http.Error(w, "Email is required.", http.StatusBadRequest)
  return
 }


 if !isValidEmailFormat(email) {
  http.Error(w, "Invalid email format.", http.StatusBadRequest)
  return
 }


 sanitizedEmail := sanitizeString(email)


 stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
 if err != nil {
  log.Println("Prepare statement failed:", err)
  http.Error(w, "Database query error", http.StatusInternalServerError)
  return
 }
 defer stmt.Close()


 result, err := stmt.Exec(sanitizedEmail)
 if err != nil {
  log.Println("Execute query failed:", err)
  http.Error(w, "Database execution error", http.StatusInternalServerError)
  return
 }


 rowsAffected, err := result.RowsAffected()
 if err != nil {
  log.Println("Get affected rows failed:", err)
  http.Error(w, "Error getting affected rows", http.StatusInternalServerError)
  return
 }


 if rowsAffected > 0 {
  fmt.Fprintln(w, "Successfully unsubscribed.")
 } else {
  fmt.Fprintln(w, "Email not found or already unsubscribed.")
 }
}


func isValidEmailFormat(email string) bool {
 _, err := mail.ParseAddress(email)
 return err == nil
}


func sanitizeString(s string) string {
 s = strings.TrimSpace(s)
 return s
}