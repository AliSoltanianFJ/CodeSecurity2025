import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

type Order struct {
    ID         int     `json:"id"`
    CustomerID string  `json:"customer_id"`
    Status     string  `json:"status"`
    Total      float64 `json:"total"`
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    customerID := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    fmt.Println("Processing order search")
    fmt.Println("Retrieving matching orders")

    // open a DB connection (in real life you’d reuse a global *sql.DB)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/yourdb")
    if err != nil {
        http.Error(w, "could not connect to db", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // query orders by customer_id and status
    rows, err := db.Query(
        "SELECT id, customer_id, status, total FROM orders WHERE customer_id = ? AND status = ?",
        customerID, status,
    )
    if err != nil {
        http.Error(w, "query error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // collect results
    var orders []Order
    for rows.Next() {
        var o Order
        if err := rows.Scan(&o.ID, &o.CustomerID, &o.Status, &o.Total); err != nil {
            http.Error(w, "scan error", http.StatusInternalServerError)
            return
        }
        orders = append(orders, o)
    }
    if err := rows.Err(); err != nil {
        http.Error(w, "row iteration error", http.StatusInternalServerError)
        return
    }

    // return as JSON
    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        http.Error(w, "encode error", http.StatusInternalServerError)
        return
    }
}