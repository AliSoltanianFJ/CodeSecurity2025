const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'");
  next();
});

// Configure MySQL connection pool (better than single connection)
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Sanitize inputs to prevent SQL injection
    // Using prepared statements with parameterized queries
    const query = 'SELECT password_hash, salt FROM users WHERE username = ?';

    db.query(query, [username], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).json({ error: "Internal server error" });
        }

        if (results.length === 0) {
            // No user found with this username
            return res.status(401).json({ error: "Invalid credentials" });
        }

        const { password_hash, salt } = results[0];

        // Hash the provided password with the stored salt
        const derivedKey = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512');
        
        if (derivedKey.toString('hex') !== password_hash) {
            return res.status(401).json({ error: "Invalid credentials" });
        }

        // Password matches - create a session
        req.session.user = {
            username: username,
            loggedIn: true
        };

        // Set secure cookie flags
        res.cookie('session-id', req.session.id, {
            httpOnly: true,
            secure: true,
            sameSite: 'lax'
        });

        return res.status(200).json({ success: true });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

