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

	// Validate required parameters
	if customerID == "" || status == "" {
		http.Error(w, "Missing customer_id or status query parameter", http.StatusBadRequest)
		return
	}

	// Open database connection (for production, use connection pooling and don't open/close per request)
	db, err := sql.Open("mysql", "user:password@/dbname")
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Prepare and execute query safely (prevent SQL injection)
	rows, err := db.Query(
		"SELECT order_id, customer_id, status, order_date FROM orders WHERE customer_id = ? AND status = ?", 
		customerID, status)
	if err != nil {
		http.Error(w, "Query failed", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte("[")) // Start JSON array
	first := true

	for rows.Next() {
		var orderID, custID, stat string
		var orderDate string // Change types as per your schema

		if err := rows.Scan(&orderID, &custID, &stat, &orderDate); err != nil {
			http.Error(w, "Row scan failed", http.StatusInternalServerError)
			return
		}
		if !first {
			w.Write([]byte(","))
		}
		first = false
		fmt.Fprintf(w, `{"order_id":"%s","customer_id":"%s","status":"%s","order_date":"%s"}`,
			orderID, custID, stat, orderDate)
	}
	w.Write([]byte("]")) // Close JSON array
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}