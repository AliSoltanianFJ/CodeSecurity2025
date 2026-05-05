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
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'mydb',
    port: process.env.DB_PORT || 3306,
    connectionLimit: 10,
    multipleStatements: false, // Prevent multiple SQL statements in one query
    charset: 'utf8mb4' // Use a character set that supports a wide range of characters
};

// Create a pool of connections
const pool = mysql.createPool(dbConfig);

// Function to handle database connections and execute queries safely
const executeQuery = (sql, values) => {
    return new Promise((resolve, reject) => {
        pool.getConnection((err, connection) => {
            if (err) {
                reject(new Error('Failed to acquire database connection.'));
                return;
            }

            connection.beginTransaction(err => {
                if (err) {
                    connection.release();
                    reject(new Error('Failed to start transaction.'));
                    return;
                }

                connection.query(sql, values, (error, results) => {
                    if (error) {
                        return connection.rollback(() => {
                            connection.release();
                            reject(new Error('Query execution failed.'));
                        });
                    }

                    connection.commit(err => {
                        if (err) {
                            return connection.rollback(() => {
                                connection.release();
                                reject(new Error('Transaction commit failed.'));
                            });
                        }

                        connection.release();
                        resolve(results);
                    });
                });
            });
        });
    });
};

// Email validation function
const isValidEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
};

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    // Input validation: Check if email is provided
    if (!email) {
        return res.status(400).send("Email is required.");
    }

    // Input validation: Check if email is valid
    if (!isValidEmail(email)) {
        return res.status(400).send("Invalid email format.");
    }

    // Sanitize the email (although parameterized queries will handle this, it's defense in depth)
    const sanitizedEmail = email.trim().toLowerCase();

    try {
        // Use parameterized query to prevent SQL injection
        const checkEmailQuery = "SELECT id FROM subscribers WHERE email = ?";
        const emailCheckResult = await executeQuery(checkEmailQuery, [sanitizedEmail]);

        if (emailCheckResult.length > 0) {
            // Email exists, proceed with unsubscription
            const unsubscribeQuery = "DELETE FROM subscribers WHERE email = ?";
            await executeQuery(unsubscribeQuery, [sanitizedEmail]);

            return res.status(200).send("Successfully unsubscribed.");
        } else {
            // Email does not exist
            return res.status(404).send("Email not found in subscription list.");
        }
    } catch (error) {
        console.error("Unsubscription failed:", error);
        return res.status(500).send("Unsubscription failed due to a server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});