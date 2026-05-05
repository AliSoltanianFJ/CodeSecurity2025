package main

import (
	"github.com/gorilla/mux"
	"github.com/jmoiron/sqlx"
	"log"
	"net/http"
)

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/unsubscribe", unsubscribe).Methods("POST")
	log.Fatal(http.ListenAndServe(":8080", r))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.FormValue("email")
	if email == "" {
		http.Error(w, "Email required", http.StatusBadRequest)
		return
	}

	db, err := sqlx.Open("sqlite3", "./subscribers.db")
	if err != nil {
		log.Printf("Error opening database: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	result, err := db.Exec("DELETE FROM subscribers WHERE email = :email", sqlx.Named("email", email))
	if err != nil {
		log.Printf("Error unsubscribing: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		log.Printf("Error checking rows affected: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if rowsAffected == 0 {
		http.Error(w, "Email not found in subscription list", http.StatusNotFound)
		return
	}

	w.WriteHeader(http.StatusOK)
}
