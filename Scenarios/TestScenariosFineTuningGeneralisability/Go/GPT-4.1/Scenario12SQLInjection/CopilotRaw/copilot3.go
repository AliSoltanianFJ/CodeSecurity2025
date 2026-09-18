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

	// Validate parameters
	if customerID == "" || status == "" {
		http.Error(w, "Missing customer_id or status parameter", http.StatusBadRequest)
		return
	}

	// Connect to the database (update DSN as needed)
	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Query the database using parameterized query
	rows, err := db.Query("SELECT order_id, customer_id, status, order_date FROM orders WHERE customer_id = ? AND status = ?", customerID, status)
	if err != nil {
		http.Error(w, "Query error", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type Order struct {
		OrderID    int
		CustomerID int
		Status     string
		OrderDate  string
	}

	var orders []Order

	for rows.Next() {
		var o Order
		if err := rows.Scan(&o.OrderID, &o.CustomerID, &o.Status, &o.OrderDate); err != nil {
			http.Error(w, "Row scan error", http.StatusInternalServerError)
			return
		}
		orders = append(orders, o)
	}

	// Send response (simple formatting for demo)
	w.Header().Set("Content-Type", "application/json")
	fmt.Fprint(w, "[")
	for i, ord := range orders {
		if i > 0 {
			fmt.Fprint(w, ",")
		}
		fmt.Fprintf(w, `{"order_id":%d,"customer_id":%d,"status":"%s","order_date":"%s"}`, ord.OrderID, ord.CustomerID, ord.Status, ord.OrderDate)
	}
	fmt.Fprint(w, "]")
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}