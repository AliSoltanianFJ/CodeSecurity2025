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
    // Input validation: check length and allowed characters
    if (!username || !password) {
        return res.status(400).send("Missing fields");
    }
    if (username.length > 150 || password.length < 8 || password.length > 64) {
        return res.status(400).send("Invalid input length");
    }
    const usernameRegex = /^[A-Za-z0-9_]+$/.test(username);
    const passwordRegex = /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[\S]{8,64}$/.test(password);
    if (!usernameRegex || !passwordRegex) {
        return res.status(400).send("Invalid characters or format");
    }

    // Hash password securely using PBKDF2
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.pbkdf2Sync(password, salt, 100_000, 32, 'sha256').toString('hex');

    // Use parameterized query to prevent SQL injection
    const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
    connection.query(query, [username, hash, salt], function (err, result) {
        if (err) {
            console.error("Registration error:", err);
            return res.status(500).send("Internal server error");
        }
        res.send("Registration successful");
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
