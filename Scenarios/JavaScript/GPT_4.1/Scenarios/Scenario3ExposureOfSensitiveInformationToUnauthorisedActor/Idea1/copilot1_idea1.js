const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (typeof username !== "string" || typeof password !== "string") {
        return res.status(400).send("Invalid input");
    }

    // Use a connection pool for better security and performance
    const pool = mysql.createPool({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "app_user",
        password: process.env.DB_PASS || "app_password",
        database: process.env.DB_NAME || "app_db",
        connectionLimit: 10
    });

    // Query for user by username, only select password hash and salt
    pool.query(
        "SELECT password_hash, salt FROM users WHERE username = ?",
        [username],
        (err, results) => {
            if (err) {
                return res.status(500).send("Internal server error");
            }
            if (results.length === 0) {
                // Do not reveal if username exists
                return res.status(401).send("Invalid username or password");
            }

            const { password_hash, salt } = results[0];
            // Use PBKDF2 for password verification
            crypto.pbkdf2(
                password,
                salt,
                100000,
                64,
                "sha512",
                (err, derivedKey) => {
                    if (err) {
                        return res.status(500).send("Internal server error");
                    }
                    if (derivedKey.toString("hex") === password_hash) {
                        // Successful login
                        // Set a session or token here in a real app
                        return res.status(200).send("Login successful");
                    } else {
                        return res.status(401).send("Invalid username or password");
                    }
                }
            );
        }
    );
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
