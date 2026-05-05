const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
require('dotenv').config(); // Load environment variables from .env file

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database configuration using environment variables
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306,
    ssl: { // Use SSL/TLS for secure connection
        rejectUnauthorized: true // Ensure the server has a valid SSL certificate
    }
};

// Create a database connection pool
const pool = mysql.createPool(dbConfig);

// Email validation function
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Input validation: Check if email is provided
    if (!email) {
        return res.status(400).send("Email is required");
    }

    // Input validation: Check if email is valid
    if (!isValidEmail(email)) {
        return res.status(400).send("Invalid email format");
    }

    // Sanitize the email using a whitelist approach
    const sanitizedEmail = email.replace(/[^a-zA-Z0-9@._-]/g, "");
    if (sanitizedEmail !== email) {
        return res.status(400).send("Invalid characters in email");
    }

    // Generate a unique salt for this operation
    const salt = crypto.randomBytes(16).toString('hex');
    const unsubscribeOperationId = crypto.createHash('sha256').update(salt + sanitizedEmail + Date.now()).digest('hex');

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Database connection error: ", err);
            return res.status(500).send("Database error occurred");
        }

        // Start a transaction to ensure atomicity
        connection.beginTransaction((transactionErr) => {
            if (transactionErr) {
                connection.release();
                console.error("Transaction error: ", transactionErr);
                return res.status(500).send("Transaction error occurred");
            }

            const checkEmailQuery = "SELECT id FROM users WHERE email = ?";
            connection.query(checkEmailQuery, [sanitizedEmail], (checkErr, results) => {
                if (checkErr) {
                    return connection.rollback(() => {
                        connection.release();
                        console.error("Database query error: ", checkErr);
                        return res.status(500).send("Database query error occurred");
                    });
                }

                if (results.length === 0) {
                    return connection.rollback(() => {
                        connection.release();
                        return res.status(404).send("Email not found");
                    });
                }

                const userId = results[0].id;
                const unsubscribeQuery = "UPDATE users SET subscribed = FALSE WHERE id = ?";
                connection.query(unsubscribeQuery, [userId], (unsubscribeErr, unsubscribeResult) => {
                    if (unsubscribeErr) {
                        return connection.rollback(() => {
                            connection.release();
                            console.error("Unsubscribe error: ", unsubscribeErr);
                            return res.status(500).send("Failed to unsubscribe");
                        });
                    }

                    // Commit the transaction
                    connection.commit((commitErr) => {
                        if (commitErr) {
                            return connection.rollback(() => {
                                connection.release();
                                console.error("Commit error: ", commitErr);
                                return res.status(500).send("Failed to commit transaction");
                            });
                        }

                        connection.release();
                        return res.status(200).send("Successfully unsubscribed");
                    });
                });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});