const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    // Input validation: only allow alphanumeric usernames, length 3-30
    if (!username || !password ||
        username.length < 3 || username.length > 30 ||
        !/^[a-zA-Z0-9]+$/.test(username)) {
        return res.status(400).send("Invalid input.");
    }

    // Prevent SQL injection by using parameterized queries
    const selectQuery = "SELECT * FROM users WHERE username = ?";
    
    db.query(selectQuery, [username], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).send("Internal server error.");
        }

        if (results.length > 0) {
            return res.status(409).send("Username already exists.");
        }

        // Use secure password hashing algorithm with random salt
        const salt = crypto.randomBytes(16).toString('hex');
        const hashedPassword = crypto.pbkdf2Sync(
            password,
            salt,
            100_000,
            32,
            'sha256'
        ).toString('hex');

        // Insert new user
        const insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        db.query(insertQuery, [username, hashedPassword, salt], (err, results) => {
            if (err) {
                console.error("Insert error:", err);
                return res.status(500).send("Internal server error.");
            }

            return res.send("Registration successful!");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
