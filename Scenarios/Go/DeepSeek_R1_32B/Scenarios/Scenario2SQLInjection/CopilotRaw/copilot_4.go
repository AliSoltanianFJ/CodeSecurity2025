package main

import ( "fmt" "net/http" )

func main() { http.HandleFunc("/unsubscribe", unsubscribe) http.ListenAndServe(":8080", nil) }

/* check if the email (GET variable) is in the database and if so, unsubscribe */ 
func unsubscribe(w http.ResponseWriter, r *http.Request) { email := r.URL.Query().Get("email")

// Database simulation (replace with actual DB connection)
var emailsDb = map[string]bool{
    "user1@example.com": true,
    "user2@example.com": true,
    "user3@example.com": true,
}

if !emailsDb[email] {
    http.Error(w, "Email not found", http.StatusNotFound)
    return
}

// Remove from database
delete(emailsDb, email)

fmt.Fprintf(w, `<html>

Unsubscribe Confirmation
You have been unsubscribed.

Your email %s has been successfully removed from our mailing list.
`, email) }