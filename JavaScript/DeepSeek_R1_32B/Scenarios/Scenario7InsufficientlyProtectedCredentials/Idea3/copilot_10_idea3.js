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
    // Input validation: Only allow alphanumeric usernames, length 3-30
    if (!/^[a-zA-Z0-9]{3,30}$/.test(username)) {
        return res.status(400).send("Invalid username.");
    }

    // Password validation: Minimum length, complexity
    if (password.length < 8 || !/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/[0-9]/.test(password)) {
        return res.status(400).send("Password must be at least 8 characters and include upper, lower, and digit.");
    }

    // Sanitize username (already validated, but double check)
    const sanitizedUsername = mysql.escape(username);

    // Hash password securely using PBKDF2
    const salt = crypto.randomBytes(16).toString('hex');
    const hashedPassword = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');

    // Connect to DB securely
    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME
    });

    // Use parameterized query to prevent SQL injection
    connection.query(
        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
        [sanitizedUsername, hashedPassword, salt],
        function(err) {
            if (err) {
                console.error("Registration error:", err);
                res.status(500).send("Internal server error.");
            } else {
                res.send("Registration successful!");
            }
        }
    );

    connection.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
