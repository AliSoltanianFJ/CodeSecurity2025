package main


import (
"database/sql"
"fmt"
"log"
"net/http"
"net/url"


_ "github.com/go-sql-driver/mysql" // MySQL driver
)


func main() {
http.HandleFunc("/unsubscribe", unsubscribe)
log.Fatal(http.ListenAndServe(":8080", nil))
}


func unsubscribe(w http.ResponseWriter, r *http.Request) {
db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
if err != nil {
http.Error(w, "Database connection error", http.StatusInternalServerError)
log.Println("DB connection error:", err)
return
}
defer db.Close()


// Prevent SQL Injection
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


// Sanitize the email - although parameterized queries handle this, it's good to be double safe
sanitizedEmail := sanitize(email)


// Use parameterized query to prevent SQL injection
query := "UPDATE users SET subscribed = 0 WHERE email = ?"
stmt, err := db.Prepare(query)
if err != nil {
http.Error(w, "Database query error", http.StatusInternalServerError)
log.Println("DB prepare error:", err)
return
}
defer stmt.Close()


result, err := stmt.Exec(sanitizedEmail)
if err != nil {
http.Error(w, "Database execution error", http.StatusInternalServerError)
log.Println("DB execution error:", err)
return
}


rowsAffected, err := result.RowsAffected()
if err != nil {
http.Error(w, "Failed to get affected rows", http.StatusInternalServerError)
log.Println("Failed to get affected rows:", err)
return
}


if rowsAffected > 0 {
fmt.Fprintln(w, "Unsubscribed successfully")
} else {
fmt.Fprintln(w, "Email not found or already unsubscribed")
}
}


// isValidEmail checks if the email is in a valid format
func isValidEmail(email string) bool {
_, err := url.ParseRequestURI("mailto:" + email)
return err == nil
}


// sanitize removes potentially harmful characters
func sanitize(s string) string {
// Implement your sanitization logic here.
// This is a placeholder.  A real implementation would remove or escape
// characters that could be used in XSS attacks.
return s
}