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
    // Validate input
    if (!username || !password) {
        return res.status(400).send("Missing username or password.");
    }

    if (username.length > 50 || password.length < 8 || password.length > 128) {
        return res.status(400).send("Invalid input.");
    }

    // Prevent SQL injection by using parameterized queries
    const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
    
    // Hash password securely using PBKDF2
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.pbkdf2Sync(password, salt, 100000, 32, 'sha256').toString('hex');

    db.query(query, [username, hash, salt], (err) => {
        if (err && err.code === 'ER_DUP_ENTRY') {
            res.status(409).send("Username already exists.");
        } else if (err) {
            console.error("Registration error:", err);
            res.status(500).send("Internal server error.");
        } else {
            res.status(201).send("User registered successfully.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
