const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// Database connection pool
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  charset: 'utf8mb4'
});

/**
 * Log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const { username, password } = req.body;
    
    // Input validation
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // Sanitize input to prevent SQL injection
    const sanitizedUsername = db.escape(username);
    const sanitizedPassword = db.escape(password);

    // Verify user credentials using prepared statements
    const query = `SELECT id, password_hash, password_salt 
                   FROM users 
                   WHERE username = ${sanitizedUsername}
                   LIMIT 1`;

    db.getConnection((err, connection) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).send("Internal server error");
        }

        try {
            connection.query(query, (error, results) => {
                connection.release();
                
                if (error) {
                    console.error("Query error:", error);
                    return res.status(500).send("Internal server error");
                }

                if (results.length === 0) {
                    return res.status(401).send("Invalid username or password");
                }

                const storedHash = results[0].password_hash;
                const storedSalt = results[0].password_salt;

                // Verify password
                const inputHash = crypto.pbkdf2Sync(password, storedSalt, 10000, 64, 'sha512').toString('hex');

                if (inputHash !== storedHash) {
                    return res.status(401).send("Invalid username or password");
                }

                // Create a session
                req.session.user = {
                    id: results[0].id,
                    username: username
                };

                // Set secure cookie options
                res.cookie('connect.sid', req.session.id, {
                    httpOnly: true,
                    secure: process.env.NODE_ENV === 'production',
                    sameSite: 'lax'
                });

                res.redirect('/');
            });
        } catch (error) {
            console.error("Login error:", error);
            return res.status(500).send("Internal server error");
        }
    });
});

// Rate limiting for login attempts
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 10, // limit each IP to 10 requests per windowMs
});

app.use('/login', limiter);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

