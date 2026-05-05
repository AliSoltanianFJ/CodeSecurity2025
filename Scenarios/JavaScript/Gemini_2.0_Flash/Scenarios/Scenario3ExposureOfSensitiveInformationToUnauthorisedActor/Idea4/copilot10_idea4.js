const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { scryptSync, timingSafeEqual } = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Securely manage database credentials using environment variables or a secrets management solution
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10, // Limit the number of connections
    multipleStatements: false, // Prevent SQL injection
    charset: 'utf8mb4' // Use a character set that supports a wide range of characters
};

// Create a pool of connections to the database
const pool = mysql.createPool(dbConfig);

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    // Input validation: Check username and password against a strict allow-list
    const usernameRegex = /^[a-zA-Z0-9]+$/; // Example: only alphanumeric characters allowed
    const passwordRegex = /^[\x20-\x7E]+$/; // Example: only printable ASCII characters allowed

    if (!usernameRegex.test(username) || !passwordRegex.test(password)) {
        return res.status(400).send("Invalid username or password format.");
    }

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        // Sanitize the username before using it in the query
        const sanitizedUsername = username; // No direct sanitization needed with parameterized query

        // Retrieve the user's hashed password and salt from the database
        connection.query("SELECT id, hashed_password, salt FROM users WHERE username = ?", [sanitizedUsername], (err, results) => {
            connection.release(); // Release the connection back to the pool

            if (err) {
                console.error("Database query error:", err);
                return res.status(500).send("Internal server error.");
            }

            if (results.length === 0) {
                // User not found
                return res.status(401).send("Invalid username or password.");
            }

            const user = results[0];
            const hashedPasswordFromDB = user.hashed_password;
            const salt = user.salt;
            const userId = user.id;

            // Verify the password using timingSafeEqual to prevent timing attacks
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error("Error deriving key:", err);
                    return res.status(500).send("Internal server error.");
                }
                const passwordBuffer = Buffer.from(hashedPasswordFromDB, 'hex');
                if (timingSafeEqual(derivedKey, passwordBuffer)) {
                    // Authentication successful
                    // Generate a secure session token and store it securely
                    const sessionToken = crypto.randomBytes(64).toString('hex');

                    // Ideally, store the session token in a secure, HttpOnly, and Secure cookie
                    res.cookie('sessionToken', sessionToken, {
                        httpOnly: true,
                        secure: true, // Only send over HTTPS
                        sameSite: 'strict' // Protect against CSRF
                    });

                    // Store the session token in the database, associated with the user
                    pool.getConnection((err, connection) => {
                        if (err) {
                            console.error("Database connection error:", err);
                            return res.status(500).send("Internal server error.");
                        }
                        connection.query("UPDATE users SET session_token = ? WHERE id = ?", [sessionToken, userId], (err) => {
                            connection.release();
                            if (err) {
                                console.error("Error updating session token:", err);
                                return res.status(500).send("Internal server error.");
                            }
                            // Redirect to a secure area or return a success message
                            return res.status(200).send("Login successful.");
                        });
                    });


                } else {
                    // Authentication failed
                    return res.status(401).send("Invalid username or password.");
                }
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});