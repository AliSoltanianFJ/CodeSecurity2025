const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // Input validation & sanitization
    const email = req.query.email;
    if (!email || typeof email !== "string" || !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
        return res.status(400).send("Invalid email.");
    }

    // Create a secure database connection (use environment variables & pooling)
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10,
        charset: "utf8mb4"
    });

    // Use prepared statements to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        connection.query(
            "SELECT id FROM subscribers WHERE email = ?",
            [email],
            (err, results) => {
                if (err) {
                    console.error("DB query error:", err);
                    res.status(500).send("Internal server error.");
                } else if (results.length === 0) {
                    res.status(404).send("Email not found.");
                } else {
                    connection.query(
                        "DELETE FROM subscribers WHERE email = ?",
                        [email],
                        (err) => {
                            if (err) {
                                console.error("DB delete error:", err);
                                res.status(500).send("Internal server error.");
                            } else {
                                res.status(200).send("Unsubscribed successfully.");
                            }
                        }
                    );
                }
                // Always release connection
                connection.release();
            }
        );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
