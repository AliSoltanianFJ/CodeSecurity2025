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
 // Connect to database
 db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
 if err != nil {
  log.Println("DB connection error:", err)
  http.Error(w, "Database connection error", http.StatusInternalServerError)
  return
 }
 defer db.Close()


 // Get email from query parameters
 email := r.URL.Query().Get("email")
 if email == "" {
  http.Error(w, "Email is required", http.StatusBadRequest)
  return
 }


 // Validate email format
 if !isValidEmail(email) {
  http.Error(w, "Invalid email format", http.StatusBadRequest)
  return
 }


 // Sanitize email input
 sanitizedEmail := sanitizeEmail(email)


 // Prepare SQL statement
 stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
 if err != nil {
  log.Println("Prepare statement error:", err)
  http.Error(w, "Database query error", http.StatusInternalServerError)
  return
 }
 defer stmt.Close()


 // Execute SQL statement
 result, err := stmt.Exec(sanitizedEmail)
 if err != nil {
  log.Println("Execute statement error:", err)
  http.Error(w, "Database execution error", http.StatusInternalServerError)
  return
 }


 // Get number of rows affected
 rowsAffected, err := result.RowsAffected()
 if err != nil {
  log.Println("Rows affected error:", err)
  http.Error(w, "Error getting affected rows", http.StatusInternalServerError)
  return
 }


 // Respond to client
 if rowsAffected > 0 {
  fmt.Fprintln(w, "Unsubscribed successfully.")
 } else {
  fmt.Fprintln(w, "Email not found or already unsubscribed.")
 }
}


// isValidEmail checks if the email address is valid
func isValidEmail(email string) bool {
 _, err := mail.ParseAddress(email)
 return err == nil
}


// sanitizeEmail removes leading/trailing spaces from the email
func sanitizeEmail(email string) string {
 return strings.TrimSpace(email)
}