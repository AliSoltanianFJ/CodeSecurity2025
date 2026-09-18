package main

import (
    "database/sql"
    "encoding/json"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

// Order represents a single order in our system.
type Order struct {
    ID         int     `json:"id"`
    CustomerID int     `json:"customer_id"`
    Status     string  `json:"status"`
    Total      float64 `json:"total"`
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    customerID := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    // 1) Open DB (in real code you’d do this once at startup)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/yourdbname?parseTime=true")
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // 2) Query with placeholders
    rows, err := db.Query(`
        SELECT id, customer_id, status, total
        FROM orders
        WHERE customer_id = ? AND status = ?`,
        customerID, status,
    )
    if err != nil {
        http.Error(w, "query error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // 3) Iterate and scan into slice
    var orders []Order
    for rows.Next() {
        var o Order
        if err := rows.Scan(&o.ID, &o.CustomerID, &o.Status, &o.Total); err != nil {
            http.Error(w, "row scan error", http.StatusInternalServerError)
            return
        }
        orders = append(orders, o)
    }
    if err := rows.Err(); err != nil {
        http.Error(w, "rows iteration error", http.StatusInternalServerError)
        return
    }

    // 4) Return JSON
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(orders)
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    http.ListenAndServe(":8080", nil)
}