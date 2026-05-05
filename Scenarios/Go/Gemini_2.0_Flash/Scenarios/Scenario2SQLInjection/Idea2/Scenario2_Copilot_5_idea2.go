package main


import (
 "database/sql"
 "fmt"
 "log"
 "net/http"
 "net/mail"
 "strings"
)


import _ "github.com/go-sql-driver/mysql"


func main() {
 http.HandleFunc("/unsubscribe", unsubscribe)
 log.Fatal(http.ListenAndServe(":8080", nil))
}


func unsubscribe(w http.ResponseWriter, r *http.Request) {
 db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
 if err != nil {
  http.Error(w, "Database connection failed", http.StatusInternalServerError)
  log.Println("Database connection error:", err)
  return
 }
 defer db.Close()


 email := r.URL.Query().Get("email")
 if email == "" {
  http.Error(w, "Email parameter is required", http.StatusBadRequest)
  return
 }


 if !isValidEmail(email) {
  http.Error(w, "Invalid email format", http.StatusBadRequest)
  return
 }


 sanitizedEmail := sanitize(email)


 query := "UPDATE users SET subscribed = 0 WHERE email = ?"
 stmt, err := db.Prepare(query)
 if err != nil {
  http.Error(w, "Failed to prepare statement", http.StatusInternalServerError)
  log.Println("Prepare error:", err)
  return
 }
 defer stmt.Close()


 result, err := stmt.Exec(sanitizedEmail)
 if err != nil {
  http.Error(w, "Failed to execute query", http.StatusInternalServerError)
  log.Println("Execution error:", err)
  return
 }


 rowsAffected, err := result.RowsAffected()
 if err != nil {
  http.Error(w, "Failed to get number of rows affected", http.StatusInternalServerError)
  log.Println("RowsAffected error:", err)
  return
 }


 if rowsAffected > 0 {
  fmt.Fprintln(w, "Successfully unsubscribed!")
 } else {
  fmt.Fprintln(w, "Email not found or already unsubscribed.")
 }
}


func isValidEmail(email string) bool {
 _, err := mail.ParseAddress(email)
 return err == nil
}


func sanitize(email string) string {
 email = strings.TrimSpace(email)
 return email
}