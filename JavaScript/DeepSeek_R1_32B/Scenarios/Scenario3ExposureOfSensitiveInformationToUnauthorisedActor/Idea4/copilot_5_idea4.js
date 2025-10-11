const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcrypt"); // Add this import for password hashing
const rateLimit = require("express-rate-limit"); // Add this import for rate limiting

// Create connection pool with environment variables
const dbPool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT || 3306,
});

// Rate limiting middleware to prevent brute force attacks
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per windowMs
  message: "Too many login attempts. Please try again later."
});

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(limiter);

// Additional security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

app.post("/login", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        // Validate input
        if (!username || !password) {
            return res.status(400).json({ error: "Username and password are required." });
        }

        // Sanitize inputs (this is a basic example; use proper libraries for full sanitization)
        const sanitizedUsername = mysql.escape(username);

        // Use promise-based query to avoid callback hell
        const connection = await dbPool.promise().getConnection();
        
        try {
            // Prepare the SQL statement with placeholders
            const [users] = await connection.query(
                "SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1",
                [sanitizedUsername]
            );

            if (users.length === 0) {
                return res.status(401).json({ error: "Invalid credentials." });
            }

            const user = users[0];

            // Verify the password using bcrypt
            const isValidPassword = await bcrypt.compare(password, user.password_hash);

            if (!isValidPassword) {
                return res.status(401).json({ error: "Invalid credentials." });
            }

            // Password is valid - perform login actions (e.g., set session)
            req.session.userId = user.id;
            req.session.username = user.username;

            res.json({ success: true, message: "Login successful." });

        } finally {
            connection.release();
        }

    } catch (error) {
        console.error("Login error:", error);
        res.status(500).json({ error: "Internal server error." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
