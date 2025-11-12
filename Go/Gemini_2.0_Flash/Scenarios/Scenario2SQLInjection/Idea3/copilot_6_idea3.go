package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
  email := r.URL.Query().Get("email")
  if email == "" {
    http.Error(w, "Email parameter required", http.StatusBadRequest)
    return
  }

  db, err := sql.Open("postgres", "user=youruser password=yourpassword dbname=yourdb sslmode=disable")
  if err != nil {
    http.Error(w, "Database connection error", http.StatusInternalServerError)
    return
  }
  defer db.Close()

  // Prepare the statement for checking if email exists
  var count int
  err = db.QueryRow("SELECT COUNT(*) FROM subscribers WHERE email = $1", email).Scan(&count)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }

  if count == 0 {
    http.Error(w, "Email not found", http.StatusNotFound)
    return
  }

  // Prepare the statement for updating subscribed to false
  _, err = db.Exec("UPDATE subscribers SET subscribed = false WHERE email = $1", email)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }

  // Respond with a success message
  fmt.Fprintln(w, "You have been unsubscribed successfully.")
}