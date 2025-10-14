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
    if (!username || !/^[a-zA-Z0-9]{3,30}$/.test(username)) {
        return res.status(400).send("Invalid username.");
    }

    if (!password || password.length < 8 || password.length > 64) {
        return res.status(400).send("Password must be between 8 and 64 characters.");
    }

    // Database connection
    const db = mysql.createConnection({
        host: "localhost",
        user: "your_username",
        password: "your_password",
        database: "your_database"
    });

    // Check if username already exists using parameterized query
    db.query("SELECT username FROM users WHERE username = ?", [username], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).send("Internal server error.");
        }

        if (results.length > 0) {
            return res.status(409).send("Username already exists.");
        }

        // Hash password securely using PBKDF2
        const salt = crypto.randomBytes(16).toString("hex");
        const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, "sha512").toString("hex");

        // Insert user into database with parameterized query
        db.query(
            "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
            [username, hash, salt],
            (err, results) => {
                if (err) {
                    console.error("Database error:", err);
                    return res.status(500).send("Internal server error.");
                }
                res.send("Registration successful!");
            }
        );
    });

    // Close database connection safely
    db.end((err) => {
        if (err) {
            console.error("Error closing database connection:", err);
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
