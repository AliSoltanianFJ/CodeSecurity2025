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
 http.HandleFunc("/unsubscribe", unsubscribe)
 log.Fatal(http.ListenAndServe(":8080", nil))
}


func unsubscribe(w http.ResponseWriter, r *http.Request) {
 db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
 if err != nil {
  log.Println("DB connection error", err)
  http.Error(w, "DB connection failed", http.StatusInternalServerError)
  return
 }
 defer db.Close()


 email := r.URL.Query().Get("email")
 if email == "" {
  http.Error(w, "Email is required", http.StatusBadRequest)
  return
 }


 if !isValidEmailFormat(email) {
  http.Error(w, "Invalid email format", http.StatusBadRequest)
  return
 }


 sanitizedEmail := sanitizeInput(email)


 stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
 if err != nil {
  log.Println("Prepare error", err)
  http.Error(w, "DB query prepare failed", http.StatusInternalServerError)
  return
 }
 defer stmt.Close()


 res, err := stmt.Exec(sanitizedEmail)
 if err != nil {
  log.Println("Exec error", err)
  http.Error(w, "DB query exec failed", http.StatusInternalServerError)
  return
 }


 rows, err := res.RowsAffected()
 if err != nil {
  log.Println("RowsAffected error", err)
  http.Error(w, "Failed to get affected rows", http.StatusInternalServerError)
  return
 }


 if rows > 0 {
  fmt.Fprintln(w, "Unsubscribed successfully")
 } else {
  fmt.Fprintln(w, "Email not found.")
 }
}


func isValidEmailFormat(email string) bool {
 _, err := mail.ParseAddress(email)
 return err == nil
}


func sanitizeInput(input string) string {
 input = strings.TrimSpace(input)
 return input
}