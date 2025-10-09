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

// Securely manage database credentials using environment variables or a dedicated secrets management solution
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10, // Limit the number of connections
    queueLimit: 0,       // Unlimited queueing
    waitForConnections: true // Wait for connections to become available
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

    // Input validation: Check username length and character set
    if (username.length < 3 || username.length > 50 || !/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).send("Invalid username format.");
    }

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        // Sanitize username before using it in the query
        const sanitizedUsername = username.replace(/[^a-zA-Z0-9_]/g, '');

        const query = "SELECT id, password_hash, salt FROM users WHERE username = ?";
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
            const passwordHash = user.password_hash;
            const salt = user.salt;
            const id = user.id;

            // Use timingSafeEqual to prevent timing attacks
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error("Error deriving key:", err);
                    return res.status(500).send("Internal server error.");
                }
                
                const derivedKeyBuffer = Buffer.from(derivedKey, 'binary');
                const passwordHashBuffer = Buffer.from(passwordHash, 'binary');

                if (timingSafeEqual(passwordHashBuffer, derivedKeyBuffer)) {
                    // Authentication successful
                    // Generate a secure session token and set it as a cookie
                    crypto.randomBytes(48, function(err, buffer) {
                        const token = buffer.toString('hex');
                        res.cookie('sessionToken', token, {
                            httpOnly: true, // Prevents client-side JavaScript access
                            secure: true,   // Only send over HTTPS
                            sameSite: 'strict' // Protects against CSRF
                        });
                        res.status(200).send("Login successful.");
                    });
                } else {
                    // Authentication failed
                    res.status(401).send("Invalid username or password.");
                }
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});