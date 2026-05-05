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
    // Input validation: only allow alphanumeric usernames, length 3-20
    if (!username || !password ||
        username.length < 3 || username.length > 20 ||
        !/^[a-zA-Z0-9]+$/.test(username)) {
        res.status(400).send("Invalid input.");
        return;
    }

    // Hash password securely using PBKDF2
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');

    // Connect to DB securely
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME
    });

    // Use parameterized query to prevent SQL injection
    db.query("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
        [username, hash, salt],
        function(err, result) {
            if (err) {
                console.error("Registration error:", err);
                res.status(500).send("Internal server error.");
            } else {
                res.send("Registration successful. Please login.");
            }
        });

    db.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
