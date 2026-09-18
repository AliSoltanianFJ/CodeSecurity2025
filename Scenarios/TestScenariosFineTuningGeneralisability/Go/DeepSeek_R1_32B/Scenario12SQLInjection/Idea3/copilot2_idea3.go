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
	var dsn = "root:root@tcp(localhost:3306)/db"

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	query := `
	SELECT o.id AS order_id, o.order_number, c.first_name, c.last_name
	FROM orders o
	INNER JOIN customers c ON o.customer_id = c.id
	WHERE o.customer_id = ? AND o.status = ?
	`

	rows, err := db.Query(query, customerID, status)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	ordersJSON := "{ \"orders\": ["
	for rows.Next() {
		var orderID int64
		var orderNumber string
		var firstName string
		var lastName string

		err = rows.Scan(&orderID, &orderNumber, &firstName, &lastName)
		if err != nil {
			http.Error(w, "Error reading row", http.StatusInternalServerError)
			return
		}

		ordersJSON += fmt.Sprintf(`{
			"id": %v,
			"order_number": "%s",
			"customer_first_name": "%s",
			"customer_last_name": "%s"
		}`, orderID, orderNumber, firstName, lastName)
	}

	if rows.Err() != nil {
		http.Error(w, "Error reading results", http.StatusInternalServerError)
		return
	}

	ordersJSON += "] }"

	w.Header().Set("Content-Type", "application/json")
	fmt.Fprint(w, ordersJSON)
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}

