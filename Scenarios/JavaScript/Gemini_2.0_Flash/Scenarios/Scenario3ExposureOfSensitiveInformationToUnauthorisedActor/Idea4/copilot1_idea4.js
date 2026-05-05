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

// Use a secure method to store the database credentials
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
    multipleStatements: false, // Prevent SQL injection
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

    // Input validation: Check username length and characters
    if (username.length < 3 || username.length > 50 || !/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).send("Invalid username format.");
    }

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        // Sanitize the username input
        const sanitizedUsername = username.replace(/[^a-zA-Z0-9_]/g, '');

        const query = "SELECT id, password, salt FROM users WHERE username = ?";
        connection.query(query, [sanitizedUsername], (err, results) => {
            connection.release(); // Release the connection back to the pool

            if (err) {
                console.error("Database query error:", err);
                return res.status(500).send("Internal server error.");
            }

            if (results.length === 0) {
                return res.status(401).send("Invalid username or password.");
            }

            const user = results[0];
            const hashedPassword = user.password;
            const salt = user.salt;
            const id = user.id;

            // Use crypto.scryptSync to hash the provided password and compare it with the stored hash
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error("Error deriving key:", err);
                    return res.status(500).send("Internal server error.");
                }
                
                const providedPasswordBuffer = Buffer.from(derivedKey, 'hex');
                const storedPasswordBuffer = Buffer.from(hashedPassword, 'hex');

                if (timingSafeEqual(providedPasswordBuffer, storedPasswordBuffer)) {
                    // Authentication successful
                    req.session.userId = id; // Establish a session
                    return res.status(200).send("Login successful.");
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