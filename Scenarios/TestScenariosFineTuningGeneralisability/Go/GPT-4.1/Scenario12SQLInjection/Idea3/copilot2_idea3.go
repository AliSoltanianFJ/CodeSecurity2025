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

    // Open the database connection (update DSN as needed)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/yourdb")
    if err != nil {
        http.Error(w, "DB connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Parameterized query to prevent SQL injection
    rows, err := db.Query(`SELECT order_id, order_date, amount FROM orders WHERE customer_id = ? AND status = ?`, customerID, status)
    if err != nil {
        http.Error(w, "Query error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    for rows.Next() {
        var orderID int
        var orderDate string
        var amount float64
        if err := rows.Scan(&orderID, &orderDate, &amount); err != nil {
            http.Error(w, "Scan error", http.StatusInternalServerError)
            return
        }
        fmt.Fprintf(w, "OrderID: %d, Date: %s, Amount: %.2f\n", orderID, orderDate, amount)
    }
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}