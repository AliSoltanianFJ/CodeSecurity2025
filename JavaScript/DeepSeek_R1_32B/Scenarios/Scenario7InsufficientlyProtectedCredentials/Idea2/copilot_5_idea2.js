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

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Sanitize input to prevent SQL injection and XSS
    const sanitizedUsername = mysql.escape(username);
    
    // Password requirements:
    // - Minimum length of 8 characters
    if (password.length < 8) {
        return res.status(400).json({ error: "Password must be at least 8 characters" });
    }

    // Hash the password using SHA-256
    const hash = crypto.createHash('sha256');
    const hashedPassword = hash.update(password).digest('hex');

    // Check if username already exists
    pool.query("SELECT * FROM users WHERE username = ?", [sanitizedUsername], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({ error: "Internal server error" });
        }

        if (results.length > 0) {
            return res.status(409).json({ error: "Username already exists" });
        }

        // Insert new user into database
        const query = "INSERT INTO users (username, password) VALUES (?, ?)";
        pool.query(query, [sanitizedUsername, hashedPassword], (err, results) => {
            if (err) {
                console.error("Registration error:", err);
                return res.status(500).json({ error: "Internal server error" });
            }

            // Return success response without sensitive data
            res.status(201).json({ 
                message: "User registered successfully",
                userId: results.insertId,
                username: sanitizedUsername
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
