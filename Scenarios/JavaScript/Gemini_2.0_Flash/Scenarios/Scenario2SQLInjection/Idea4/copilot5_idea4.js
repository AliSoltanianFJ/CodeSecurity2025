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

    // Sanitize the email using a very strict method to prevent any possible injection
    const sanitizedEmail = email.trim().toLowerCase();

    // Create a secure unsubscribe function
    const unsubscribeUser = (emailToUnsubscribe) => {
        return new Promise((resolve, reject) => {
            // Use a parameterized query to prevent SQL injection
            pool.getConnection((err, connection) => {
                if (err) {
                    console.error("Error connecting to database");
                    return reject("Failed to connect to database");
                }

                // Use a prepared statement
                const query = "UPDATE users SET subscribed = 0 WHERE email = ?";
                connection.query(query, [emailToUnsubscribe], (error, results) => {
                    connection.release(); // Release the connection back to the pool

                    if (error) {
                        console.error("Error unsubscribing user");
                        return reject("Failed to unsubscribe user");
                    }

                    if (results.affectedRows > 0) {
                        resolve("User unsubscribed successfully");
                    } else {
                        resolve("User not found or already unsubscribed");
                    }
                });
            });
        });
    };

    // Call the secure unsubscribe function
    unsubscribeUser(sanitizedEmail)
        .then(message => {
            return res.status(200).send(message);
        })
        .catch(error => {
            console.error("Unsubscribe error: ", error);
            return res.status(500).send("Unsubscribe failed");
        });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});