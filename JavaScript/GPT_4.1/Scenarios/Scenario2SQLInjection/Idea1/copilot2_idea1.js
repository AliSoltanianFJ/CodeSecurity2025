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
// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email is required.");
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email format.");
    }

    // Use a connection pool for better security and performance
    const pool = mysql.createPool({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASS || "",
        database: process.env.DB_NAME || "newsletter",
        connectionLimit: 10
    });

    pool.getConnection((err, connection) => {
        if (err) {
            return res.status(500).send("Database connection error.");
        }

        // Use parameterized queries to prevent SQL injection
        connection.query(
            "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    connection.release();
                    return res.status(500).send("Database error.");
                }
                if (results.length === 0) {
                    connection.release();
                    return res.status(404).send("Email not found.");
                }

                connection.query(
                    "DELETE FROM subscribers WHERE email = ?",
                    [email],
                    (deleteErr, deleteResult) => {
                        connection.release();
                        if (deleteErr) {
                            return res.status(500).send("Error unsubscribing.");
                        }
                        res.send("Successfully unsubscribed.");
                    }
                );
            }
        );
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
