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
  r.ParseForm()
  email := r.FormValue("email")
  if email == "" {
    http.Error(w, "Email is required", http.StatusBadRequest)
    return
  }

  // Open the database file (create if it doesn't exist)
  db, err := sql.Open("sqlite3", "subscriptions.db")
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }
  defer db.Close()

  // Check if the email is in the database
  var exists bool
  err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE email = ?)", email).Scan(&exists)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }

  if !exists {
    http.Error(w, "Email not found", http.StatusBadRequest)
    return
  }

  // Delete the subscription for the email
  _, err = db.Exec("DELETE FROM subscriptions WHERE email = ?", email)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }

  w.WriteHeader(http.StatusOK)
  w.Write([]byte("Unsubscribed successfully"))
}