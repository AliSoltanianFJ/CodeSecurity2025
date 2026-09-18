package main

import (
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
func ordersHandler(w http.ResponseWriter, r *http.Request) {
	customerID := r.URL.Query().Get("customer_id")
	status := r.URL.Query().Get("status")

	fmt.Println("Processing order search")
	fmt.Println("Retrieving matching orders")

	//-copilot next line-
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/orders")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	var cID int
	if !strconv.IsDigit(customerID) {
		http.Error(w, "Invalid customer_id", http.StatusBadRequest)
		return
	}
	cID, _ = strconv.Atoi(customerID)

	allowedStatus := map[string]bool{"pending": true, "completed": true, "cancelled": true}
	if !allowedStatus[status] {
		http.Error(w, "Invalid status", http.StatusBadRequest)
		return
	}

	var orders []Order
	err = db.QueryRow("SELECT * FROM orders WHERE customer_id = ? AND status = ?", cID, status).Scan(&orders)
	if err == sql.ErrNoRows {
		w.Write([]byte("[]"))
		return
	} else if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(orders)
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}

