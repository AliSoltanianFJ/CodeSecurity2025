package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "strconv"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

// Order represents one row in our "orders" table.
type Order struct {
    ID         int       `json:"id"`
    CustomerID int       `json:"customer_id"`
    Status     string    `json:"status"`
    Total      float64   `json:"total"`
    CreatedAt  time.Time `json:"created_at"`
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Pull out the two query parameters
    customerIDParam := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    // 2) Both are required
    if customerIDParam == "" || status == "" {
        http.Error(w, "missing customer_id or status", http.StatusBadRequest)
        return
    }

    // 3) Parse customer_id as an integer
    customerID, err := strconv.Atoi(customerIDParam)
    if err != nil {
        http.Error(w, "customer_id must be an integer", http.StatusBadRequest)
        return
    }

    // 4) Open (or reuse) your database handle
    //    In production you’d probably do this once in main() and reuse the *sql.DB
    //    here for the lifetime of the application.
    db, err := sql.Open(
        "mysql",
        "username:password@tcp(127.0.0.1:3306)/yourdbname?parseTime=true",
    )
    if err != nil {
        log.Printf("db.Open error: %v", err)
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // 5) Prepare a statement with placeholders (?), never interpolate strings directly!
    stmt, err := db.Prepare(`
        SELECT
            id,
            customer_id,
            status,
            total,
            created_at
        FROM orders
        WHERE customer_id = ?
          AND status = ?
    `)
    if err != nil {
        log.Printf("db.Prepare error: %v", err)
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    // 6) Execute the query, passing customerID and status as parameters
    rows, err := stmt.Query(customerID, status)
    if err != nil {
        log.Printf("stmt.Query error: %v", err)
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // 7) Iterate the results and scan each row into an Order.
    orders := make([]Order, 0)
    for rows.Next() {
        var order Order
        if err := rows.Scan(
            &order.ID,
            &order.CustomerID,
            &order.Status,
            &order.Total,
            &order.CreatedAt,
        ); err != nil {
            log.Printf("rows.Scan error: %v", err)
            http.Error(w, "database error", http.StatusInternalServerError)
            return
        }
        orders = append(orders, order)
    }
    if err := rows.Err(); err != nil {
        log.Printf("rows iteration error: %v", err)
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    // 8) Return the results as JSON.
    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        log.Printf("json.Encode error: %v", err)
    }
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}
    
