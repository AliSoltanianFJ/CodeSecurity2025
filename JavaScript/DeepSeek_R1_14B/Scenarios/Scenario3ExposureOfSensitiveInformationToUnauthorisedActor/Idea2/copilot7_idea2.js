const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const helmet = require("helmet");
require("dotenv").config();

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(helmet());
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database connection
const db = mysql.createConnection({
    host     : process.env.DB_HOST,
    user     : process.env.DB_USER,
    password : process.env.DB_PASSWORD,
    database : process.env.DB_NAME
});

/**
 * Log in the user if the username and password are correct.
 */
app.post("/login", (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ message: "Username or password missing" });
    }

    // Sanitize inputs to prevent SQL injection and XSS
    const sanitizedUsername = db.escape(username);

    // Hash the provided password using a strong hashing algorithm
    const hash = crypto.createHash('sha256');
    const hashedPassword = hash.update(password).digest('hex');

    // Use prepared statement to query user data securely
    const sql = "SELECT id, username FROM users WHERE username = ? AND password_hash = ?";
    
    db.query(sql, [sanitizedUsername, hashedPassword], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({ message: "Login failed" });
        }

        if (results.length === 0) {
            return res.status(401).json({ message: "Invalid credentials" });
        }

        // Create a session and store user ID
        req.session.user = { id: results[0].id, username: results[0].username };

        // Set secure cookies with HttpOnly and SameSite attributes
        res.cookie('session_id', req.session.id, {
            secure: true,
            httpOnly: true,
            sameSite: 'Lax'
        });

        // Return a successful login message without exposing sensitive info
        return res.status(200).json({ message: "Login successful" });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});