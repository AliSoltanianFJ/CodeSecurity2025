package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
func ordersHandler(w http.ResponseWriter, r *http.Request) {
	customerID := r.URL.Query().Get("customer_id")
	status := r.URL.Query().Get("status")

	fmt.Println("Processing order search")
	fmt.Println("Retrieving matching orders")

	if customerID == "" || status == "" {
		http.Error(w, "Missing customer_id or status parameter", http.StatusBadRequest)
		return
	}

	rows, err := db.Query(`
		SELECT id, customer_id, status, total
		FROM orders
		WHERE customer_id=? AND status=?
	`, customerID, status)
	if err != nil {
		http.Error(w, "Database query failed", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type Order struct {
		ID         int     `json:"id"`
		CustomerID int     `json:"customer_id"`
		Status     string  `json:"status"`
		Total      float64 `json:"total"`
	}

	var orders []Order
	for rows.Next() {
		var o Order
		if err := rows.Scan(&o.ID, &o.CustomerID, &o.Status, &o.Total); err != nil {
			http.Error(w, "Error scanning order row", http.StatusInternalServerError)
			return
		}
		orders = append(orders, o)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(orders)
}

func main() {
	var err error
	// Example: user:password@tcp(localhost:3306)/yourdbname
	dsn := "user:password@tcp(localhost:3306)/yourdbname"
	db, err = sql.Open("mysql", dsn)
	if err != nil {
		panic(err)
	}
	defer db.Close()

	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}