const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs"); // Ensure you install this package
const session = require("express-session"); // Install and configure

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Configure session middleware
app.use(session({
    secret: 'your-secret-key-here', 
    resave: false,
    saveUninitialized: true,
    cookie: {
        secure: true,  // Force HTTPS only (if in production)
        httpOnly: true, 
        sameSite: 'lax'
    }
}));

// Database connection setup
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", async (req, res) => {
    try {
        const { username, password } = req.body;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ error: "Username and password are required." });
        }

        // Check for reasonable lengths
        if (username.length > 50 || password.length < 6 || password.length > 100) {
            return res.status(400).json({ error: "Invalid credentials format." });
        }

        // SQL Injection prevention using prepared statements
        db.query(
            'SELECT id, username, hash FROM users WHERE username = ?',
            [username],
            async (err, results) => {
                if (err) {
                    console.error("Database error:", err);
                    return res.status(500).json({ error: "Internal server error." });
                }

                // Check if user exists
                if (results.length === 0) {
                    return res.status(401).json({ error: "Invalid credentials." });
                }

                const { id, username: storedUsername, hash } = results[0];

                // Password verification using bcrypt
                const isMatch = await bcrypt.compare(password, hash);

                if (!isMatch) {
                    return res.status(401).json({ error: "Invalid credentials." });
                }

                // Secure session management
                req.session.userId = id;
                req.session.username = storedUsername;
                req.sessionloggedIn = true;

                // Optional: Implement CAPTCHA for repeated failed attempts

                return res.json({ 
                    success: true, 
                    message: "Successfully logged in."
                });

            }
        );

    } catch (error) {
        console.error("Login error:", error);
        res.status(500).json({ error: "Internal server error." });
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
