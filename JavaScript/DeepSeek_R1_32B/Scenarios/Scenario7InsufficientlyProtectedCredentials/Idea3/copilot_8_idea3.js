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
    // Input validation: Only allow alphanumeric usernames of appropriate length
    if (!username || !password) {
        return res.status(400).send("Missing fields");
    }
    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send("Invalid input type");
    }
    if (username.length < 3 || username.length > 20) {
        return res.status(400).send("Username must be between 3 and 20 characters");
    }
    if (!/^[a-zA-Z0-9]*$/.test(username)) {
        return res.status(400).send("Invalid username format");
    }

    // Password validation
    if (password.length < 8 || password.length > 50) {
        return res.status(400).send("Password must be between 8 and 50 characters");
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]+$/.test(password)) {
        return res.status(400).send("Password must contain at least one letter and one number");
    }

    // Hash password securely using PBKDF2
    const salt = crypto.randomBytes(16).toString('hex');
    const hashedPassword = crypto.pbkdf2Sync(
        password,
        salt,
        100000,
        64,
        'sha512'
    ).toString('hex');

    // Connect to DB securely
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME
    });

    // Use parameterized query to prevent SQL injection
    const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
    db.query(query, [username, hashedPassword, salt], function(err, result) {
        if (err) {
            console.error("Registration error:", err);
            return res.status(500).send("Internal server error");
        }
        res.status(200).send("User registered successfully");
    });

    db.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
