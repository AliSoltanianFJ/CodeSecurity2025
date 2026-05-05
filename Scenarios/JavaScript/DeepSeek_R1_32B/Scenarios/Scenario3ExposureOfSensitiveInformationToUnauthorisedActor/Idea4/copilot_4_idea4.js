const express = require("express");
const mysql = require("mysql2/promise"); // Use promise-based mysql
const bcrypt = require("bcryptjs");
const crypto = require("crypto");
require('dotenv').config(); // For environment variables

// Configure MySQL pool with proper security settings
const dbPool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0 // Reject connections if pool is full
});

// CSRF protection middleware
const csrf = require('csurf');
const csrfProtection = csrf({ cookie: true });
app.use(csrfProtection);

app.post("/login", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        // Input validation
        if (!username || !password) {
            return res.status(400).send("Username and password are required");
        }

        if (typeof username !== 'string' || typeof password !== 'string') {
            return res.status(400).send("Invalid input types");
        }

        // Validate length to prevent potential attacks
        if (username.length > 50 || password.length > 72) { 
            return res.status(400).send("Invalid username or password length");
        }

        // SQL injection prevention using prepared statements
        const [user] = await dbPool.query(
            'SELECT * FROM users WHERE username = ?', [username]
        );

        if (!user) {
            // Return generic error to prevent username enumeration
            return res.status(401).send("Invalid credentials");
        }

        // Password comparison using bcrypt
        const isValidPassword = await bcrypt.compare(password, user.password_hash);

        if (!isValidPassword) {
            return res.status(401).send("Invalid credentials");
        }

        // Session management with secure settings
        req.session.user = {
            id: user.id,
            username: user.username,
            isAdmin: user.is_admin
        };
        
        // Set session cookie with security options
        const sessionOptions = {
            sameSite: 'lax',
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production', // HTTPS required in production
            maxAge: 1000 * 60 * 60 * 24 * 7, // 7 days
        };

        res.cookie('session', req.sessionID, sessionOptions);

        // Return success response
        return res.status(200).json({ 
            message: "Login successful",
            user: {
                id: user.id,
                username: user.username,
                roles: user.roles || []
            }
        });

    } catch (error) {
        console.error("Login error:", error);
        // Prevent information disclosure in error messages
        return res.status(500).send("An error occurred during login");
    }
});

// Other security middleware and configurations
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline'; img-src 'self' https:");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

