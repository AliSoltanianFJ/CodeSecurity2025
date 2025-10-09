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
    database: process.env.DB_NAME
};

// Create a database connection pool
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
    if (username.length < 3 || username.length > 20 || !/^[a-zA-Z0-9]+$/.test(username)) {
        return res.status(400).send("Invalid username format.");
    }

    // Input validation: Check password length
    if (password.length < 8 || password.length > 64) {
        return res.status(400).send("Invalid password length.");
    }

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        const query = "SELECT id, password, salt FROM users WHERE username = ?";
        connection.query(query, [username], (err, results) => {
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

            // Use timingSafeEqual to prevent timing attacks
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error("Error deriving key:", err);
                    return res.status(500).send("Internal server error.");
                }
                
                const passwordBuffer = Buffer.from(hashedPassword, 'hex');
                if (timingSafeEqual(derivedKey, passwordBuffer)) {
                    // Authentication successful
                    // Generate a secure session token and set it as a cookie
                    crypto.randomBytes(48, (err, buffer) => {
                        if (err) {
                            console.error("Error generating session token:", err);
                            return res.status(500).send("Internal server error.");
                        }
                        const sessionToken = buffer.toString('hex');

                        // Store the session token in the database, associated with the user
                        const updateQuery = "UPDATE users SET session_token = ? WHERE id = ?";
                        pool.getConnection((err, connection) => {
                            if (err) {
                                console.error("Database connection error:", err);
                                return res.status(500).send("Internal server error.");
                            }
                            connection.query(updateQuery, [sessionToken, user.id], (err) => {
                                connection.release();
                                if (err) {
                                    console.error("Error updating session token:", err);
                                    return res.status(500).send("Internal server error.");
                                }

                                // Set the session token as a cookie with HTTPOnly and Secure flags
                                res.cookie('sessionToken', sessionToken, {
                                    httpOnly: true,
                                    secure: true, // Only send over HTTPS
                                    sameSite: 'strict' // Protect against CSRF
                                });

                                return res.status(200).send("Login successful.");
                            });
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