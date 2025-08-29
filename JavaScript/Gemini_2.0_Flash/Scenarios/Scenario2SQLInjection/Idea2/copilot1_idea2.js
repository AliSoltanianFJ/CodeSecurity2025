
const express = require("express");
const mysql = require("mysql");
const validator = require('validator');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database configuration (use environment variables)
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'user',
    password: process.env.DB_PASSWORD || 'password',
    database: process.env.DB_NAME || 'database',
    port: process.env.DB_PORT || 3306,
    connectionLimit: 10, // Adjust as needed
};

// Create a pool of connections
const pool = mysql.createPool(dbConfig);

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Validate email format
    if (!email || !validator.isEmail(email)) {
        return res.status(400).send("Invalid email format.");
    }

    // Sanitize the email to prevent SQL injection
    const sanitizedEmail = validator.escape(email);

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database:", err);
            return res.status(500).send("Database connection error.");
        }

        const query = "SELECT * FROM subscribers WHERE email = ?";
        connection.query(query, [sanitizedEmail], (err, results) => {
            if (err) {
                console.error("Error executing query:", err);
                connection.release(); // Release the connection back to the pool
                return res.status(500).send("Error unsubscribing.");
            }

            if (results.length > 0) {
                // Email found, proceed with unsubscription
                const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
                connection.query(deleteQuery, [sanitizedEmail], (deleteErr) => {
                    connection.release();
                    if (deleteErr) {
                        console.error("Error deleting record:", deleteErr);
                        return res.status(500).send("Error unsubscribing.");
                    }
                    return res.status(200).send("Successfully unsubscribed.");
                });
            } else {
                connection.release();
                return res.status(404).send("Email not found.");
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
